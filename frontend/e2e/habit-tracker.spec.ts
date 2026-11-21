import { test, expect } from '@playwright/test';

test.describe('Habit Tracker E2E Journey', () => {
  test.beforeEach(async ({ page }) => {
    // State management for deterministic mock backend in E2E tests
    let habits: any[] = [];
    let isCheckedInToday = false;
    let currentStreak = 0;
    const todayStr = new Date().toISOString().split('T')[0];

    // Intercept API routes
    await page.route('**/api/v1/auth/register', async (route) => {
      const body = route.request().postDataJSON();
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            accessToken: 'mock-jwt-token-xyz',
            refreshToken: 'mock-refresh-token-xyz',
            tokenType: 'Bearer',
            expiresInMs: 86400000,
            user: {
              id: 1,
              name: body.name || 'Demo User',
              email: body.email,
              createdAt: new Date().toISOString(),
            },
          },
        }),
      });
    });

    await page.route('**/api/v1/auth/login', async (route) => {
      const body = route.request().postDataJSON();
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            accessToken: 'mock-jwt-token-xyz',
            refreshToken: 'mock-refresh-token-xyz',
            tokenType: 'Bearer',
            expiresInMs: 86400000,
            user: {
              id: 1,
              name: 'Demo User',
              email: body.email,
              createdAt: new Date().toISOString(),
            },
          },
        }),
      });
    });

    await page.route('**/api/v1/habits', async (route) => {
      if (route.request().method() === 'POST') {
        const body = route.request().postDataJSON();
        const newHabit = {
          id: habits.length + 1,
          userId: 1,
          name: body.name,
          description: body.description || '',
          frequencyType: body.frequencyType,
          daysOfWeek: body.daysOfWeek,
          targetTime: body.targetTime,
          color: body.color || '#10B981',
          archived: false,
          createdAt: new Date().toISOString(),
        };
        habits.push(newHabit);
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newHabit }),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: habits }),
        });
      }
    });

    await page.route('**/api/v1/habits/*/logs/*', async (route) => {
      const body = route.request().postDataJSON();
      isCheckedInToday = !!body.completed;
      currentStreak = isCheckedInToday ? 1 : 0;

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            id: 1,
            habitId: 1,
            logDate: todayStr,
            completed: isCheckedInToday,
            note: body.note || null,
          },
        }),
      });
    });

    await page.route('**/api/v1/dashboard/summary', async (route) => {
      const todayHabits = habits.map((h) => ({
        habit: h,
        completedToday: isCheckedInToday,
        noteToday: isCheckedInToday ? 'Felt great!' : null,
        currentStreak: currentStreak,
      }));

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            todayDate: todayStr,
            todayCompletedCount: isCheckedInToday ? habits.length : 0,
            todayScheduledCount: habits.length,
            overallCompletionRate: habits.length > 0 ? (isCheckedInToday ? 100.0 : 0.0) : 0.0,
            todayHabits: todayHabits,
            weeklyOverview: [
              { date: todayStr, dayOfWeek: 'Today', completedCount: isCheckedInToday ? 1 : 0, scheduledCount: 1 },
            ],
          },
        }),
      });
    });

    await page.route('**/api/v1/dashboard/heatmap*', async (route) => {
      const days = [];
      const now = new Date();
      for (let i = 27; i >= 0; i--) {
        const d = new Date(now);
        d.setDate(d.getDate() - i);
        const dStr = d.toISOString().split('T')[0];
        const isToday = dStr === todayStr;
        days.push({
          date: dStr,
          completedCount: isToday && isCheckedInToday ? 1 : 0,
          scheduledCount: 1,
          level: isToday && isCheckedInToday ? 4 : 0,
          scheduled: true,
        });
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            startDate: days[0].date,
            endDate: days[days.length - 1].date,
            totalCompleted: isCheckedInToday ? 1 : 0,
            totalScheduled: days.length,
            days: days,
          },
        }),
      });
    });
  });

  test('Full User Journey: Sign Up -> Create Habit -> Check In -> Verify Streak & Heatmap -> Retroactive Check-in', async ({
    page,
  }) => {
    // 1. Visit App and see Authentication screen
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Habit Tracker' })).toBeVisible();

    // 2. Switch to Sign Up tab
    await page.getByRole('button', { name: 'Sign Up' }).click();

    // Fill registration form
    await page.getByPlaceholder('Alex Smith').fill('Jane Doe');
    await page.getByPlaceholder('alex@example.com').fill('jane@example.com');
    await page.getByPlaceholder('••••••••').fill('password123');
    await page.getByRole('button', { name: 'Create Account' }).click();

    // 3. User is logged in and lands on Dashboard
    await expect(page.getByText('Jane Doe')).toBeVisible();
    await expect(page.getByText("Today's Progress")).toBeVisible();
    await expect(page.getByText('No habits scheduled for today')).toBeVisible();

    // 4. Click 'Create Habit'
    await page.getByRole('button', { name: 'Create Habit' }).click();
    await expect(page.getByRole('heading', { name: 'Create New Habit' })).toBeVisible();

    // Fill habit details
    await page.getByPlaceholder(/Morning Jog/i).fill('Morning Yoga');
    await page.getByPlaceholder(/Why are you doing/i).fill('Daily flexibility & mindfulness');
    await page.locator('form').getByRole('button', { name: 'Create Habit' }).click();

    // 5. Verify Habit Card appears on dashboard
    await expect(page.getByRole('heading', { name: 'Morning Yoga' })).toBeVisible();
    await expect(page.getByText('0 days').first()).toBeVisible();

    // 6. Perform Daily Check-In
    const checkButton = page.getByTitle('Check in for today');
    await expect(checkButton).toBeVisible();
    await checkButton.click();

    // 7. Verify Streak & Progress update
    await expect(page.getByText('1 day').first()).toBeVisible();
    await expect(page.getByText('1 / 1')).toBeVisible();

    // 8. Verify Heatmap shows consistency
    await expect(page.getByText('Habit Heatmap Matrix')).toBeVisible();
    await expect(page.getByText('1 active days in this range')).toBeVisible();

    // 9. Click a square on the Heatmap to test retroactive inspection/edit modal
    const square = page.locator('div.cursor-pointer').last();
    await square.click();

    // Verify day check-in modal opens
    await expect(page.getByText(/Check-ins for/i)).toBeVisible();
    await expect(page.locator('.fixed').getByText('Morning Yoga')).toBeVisible();

    // Close modal
    await page.locator('.fixed').getByRole('button').first().click();

    // 10. Logout
    await page.getByTitle('Log out').click();
    await expect(page.getByRole('button', { name: 'Sign In' })).toBeVisible();
  });
});
