package com.habittracker.habit;

import com.habittracker.habit.dto.HabitStatsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StreakCalculatorTest {

    private StreakCalculator streakCalculator;

    @BeforeEach
    void setUp() {
        streakCalculator = new StreakCalculator();
    }

    @Test
    @DisplayName("Daily habit: active streak when completed through today")
    void shouldCalculateActiveStreakForDailyHabit() {
        Habit habit = Habit.builder()
                .id(1L)
                .frequencyType(FrequencyType.DAILY)
                .build();

        LocalDate today = LocalDate.of(2026, 3, 15); // Sunday
        LocalDate createdDate = today.minusDays(10);

        List<HabitLog> logs = new ArrayList<>();
        // Completed last 4 days: 12th, 13th, 14th, 15th (today)
        logs.add(HabitLog.builder().habitId(1L).logDate(today.minusDays(3)).completed(true).build());
        logs.add(HabitLog.builder().habitId(1L).logDate(today.minusDays(2)).completed(true).build());
        logs.add(HabitLog.builder().habitId(1L).logDate(today.minusDays(1)).completed(true).build());
        logs.add(HabitLog.builder().habitId(1L).logDate(today).completed(true).build());

        HabitStatsDto stats = streakCalculator.calculateStats(habit, logs, today, createdDate);

        assertEquals(4, stats.getCurrentStreak());
        assertEquals(4, stats.getLongestStreak());
        assertEquals(4, stats.getTotalCompletedDays());
        assertEquals(11, stats.getTotalScheduledDays()); // 11 days total (today - 10 to today inclusive)
    }

    @Test
    @DisplayName("Daily habit: streak preserved from yesterday when today is not completed yet")
    void shouldPreserveStreakWhenTodayPending() {
        Habit habit = Habit.builder()
                .id(1L)
                .frequencyType(FrequencyType.DAILY)
                .build();

        LocalDate today = LocalDate.of(2026, 3, 15);
        LocalDate createdDate = today.minusDays(5);

        List<HabitLog> logs = new ArrayList<>();
        // Completed yesterday and day before, but not today yet
        logs.add(HabitLog.builder().habitId(1L).logDate(today.minusDays(2)).completed(true).build());
        logs.add(HabitLog.builder().habitId(1L).logDate(today.minusDays(1)).completed(true).build());

        HabitStatsDto stats = streakCalculator.calculateStats(habit, logs, today, createdDate);

        // Current streak should be 2 because yesterday was completed and today isn't over/broken
        assertEquals(2, stats.getCurrentStreak());
    }

    @Test
    @DisplayName("Daily habit: missed yesterday resets current streak to 0")
    void shouldResetCurrentStreakWhenYesterdayMissed() {
        Habit habit = Habit.builder()
                .id(1L)
                .frequencyType(FrequencyType.DAILY)
                .build();

        LocalDate today = LocalDate.of(2026, 3, 15);
        LocalDate createdDate = today.minusDays(10);

        List<HabitLog> logs = new ArrayList<>();
        // Completed 3 days ago, but missed yesterday and day before
        logs.add(HabitLog.builder().habitId(1L).logDate(today.minusDays(4)).completed(true).build());
        logs.add(HabitLog.builder().habitId(1L).logDate(today.minusDays(3)).completed(true).build());
        // today.minusDays(2) missed
        // today.minusDays(1) missed
        // today missed

        HabitStatsDto stats = streakCalculator.calculateStats(habit, logs, today, createdDate);

        assertEquals(0, stats.getCurrentStreak());
        assertEquals(2, stats.getLongestStreak());
    }

    @Test
    @DisplayName("Weekday-only habit: weekend (Sat/Sun) does NOT break streak")
    void shouldNotBreakStreakOverWeekendForWeekdayHabit() {
        Habit habit = Habit.builder()
                .id(2L)
                .frequencyType(FrequencyType.WEEKDAYS)
                .build();

        // 2026-03-16 is Monday. Friday is 2026-03-13.
        LocalDate monday = LocalDate.of(2026, 3, 16);
        LocalDate createdDate = LocalDate.of(2026, 3, 1);

        List<HabitLog> logs = new ArrayList<>();
        // Completed Thursday (12th) and Friday (13th)
        logs.add(HabitLog.builder().habitId(2L).logDate(LocalDate.of(2026, 3, 12)).completed(true).build());
        logs.add(HabitLog.builder().habitId(2L).logDate(LocalDate.of(2026, 3, 13)).completed(true).build());
        // Completed Monday (16th)
        logs.add(HabitLog.builder().habitId(2L).logDate(monday).completed(true).build());

        HabitStatsDto stats = streakCalculator.calculateStats(habit, logs, monday, createdDate);

        // Thursday + Friday + Monday = 3 consecutive scheduled days
        assertEquals(3, stats.getCurrentStreak());
        assertEquals(3, stats.getLongestStreak());
    }

    @Test
    @DisplayName("Weekday-only habit: on Sunday, streak is preserved from Friday")
    void shouldPreserveWeekdayStreakOnSunday() {
        Habit habit = Habit.builder()
                .id(2L)
                .frequencyType(FrequencyType.WEEKDAYS)
                .build();

        // 2026-03-15 is Sunday. Friday is 2026-03-13.
        LocalDate sunday = LocalDate.of(2026, 3, 15);
        LocalDate createdDate = LocalDate.of(2026, 3, 1);

        List<HabitLog> logs = new ArrayList<>();
        // Completed Thursday and Friday
        logs.add(HabitLog.builder().habitId(2L).logDate(LocalDate.of(2026, 3, 12)).completed(true).build());
        logs.add(HabitLog.builder().habitId(2L).logDate(LocalDate.of(2026, 3, 13)).completed(true).build());

        HabitStatsDto stats = streakCalculator.calculateStats(habit, logs, sunday, createdDate);

        // Streak is 2 because Sunday is not a scheduled day, and Friday was completed
        assertEquals(2, stats.getCurrentStreak());
    }

    @Test
    @DisplayName("Specific-days habit: unscheduled days do not break streak")
    void shouldCalculateStreakForSpecificDaysHabit() {
        Habit habit = Habit.builder()
                .id(3L)
                .frequencyType(FrequencyType.SPECIFIC_DAYS)
                .daysOfWeek("MON,WED,FRI")
                .build();

        // 2026-03-18 is Wednesday. Previous scheduled was Monday 2026-03-16.
        LocalDate wednesday = LocalDate.of(2026, 3, 18);
        LocalDate monday = LocalDate.of(2026, 3, 16);
        LocalDate fridayBefore = LocalDate.of(2026, 3, 13);
        LocalDate createdDate = LocalDate.of(2026, 3, 1);

        List<HabitLog> logs = new ArrayList<>();
        logs.add(HabitLog.builder().habitId(3L).logDate(fridayBefore).completed(true).build());
        logs.add(HabitLog.builder().habitId(3L).logDate(monday).completed(true).build());
        logs.add(HabitLog.builder().habitId(3L).logDate(wednesday).completed(true).build());

        HabitStatsDto stats = streakCalculator.calculateStats(habit, logs, wednesday, createdDate);

        // Fri + Mon + Wed = 3 consecutive scheduled days
        assertEquals(3, stats.getCurrentStreak());
        assertEquals(3, stats.getLongestStreak());
    }

    @Test
    @DisplayName("Empty logs returns 0 streak and 0.0 completion rate")
    void shouldHandleEmptyLogs() {
        Habit habit = Habit.builder()
                .id(4L)
                .frequencyType(FrequencyType.DAILY)
                .build();

        LocalDate today = LocalDate.of(2026, 3, 15);
        HabitStatsDto stats = streakCalculator.calculateStats(habit, List.of(), today, today);

        assertEquals(0, stats.getCurrentStreak());
        assertEquals(0, stats.getLongestStreak());
        assertEquals(0.0, stats.getCompletionRate());
        assertEquals(0, stats.getTotalCompletedDays());
        assertEquals(1, stats.getTotalScheduledDays());
    }
}
