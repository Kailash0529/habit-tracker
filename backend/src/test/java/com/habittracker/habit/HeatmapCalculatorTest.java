package com.habittracker.habit;

import com.habittracker.habit.dto.HeatmapDayDto;
import com.habittracker.habit.dto.HeatmapResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HeatmapCalculatorTest {

    private HeatmapCalculator heatmapCalculator;

    @BeforeEach
    void setUp() {
        StreakCalculator streakCalculator = new StreakCalculator();
        heatmapCalculator = new HeatmapCalculator(streakCalculator);
    }

    @Test
    @DisplayName("Intensity levels: boundaries at 0%, 25%, 50%, 75%, 100%")
    void shouldCalculateCorrectIntensityLevels() {
        // Level 0: 0 completed out of 4 scheduled
        assertEquals(0, heatmapCalculator.calculateIntensityLevel(0, 4));
        assertEquals(0, heatmapCalculator.calculateIntensityLevel(0, 0));

        // Level 1: 1 of 4 (25.0%)
        assertEquals(1, heatmapCalculator.calculateIntensityLevel(1, 4));

        // Level 2: 2 of 4 (50.0%)
        assertEquals(2, heatmapCalculator.calculateIntensityLevel(2, 4));

        // Level 3: 3 of 4 (75.0%)
        assertEquals(3, heatmapCalculator.calculateIntensityLevel(3, 4));

        // Level 4: 4 of 4 (100.0%)
        assertEquals(4, heatmapCalculator.calculateIntensityLevel(4, 4));
        // Level 4: 8 of 10 (80.0%)
        assertEquals(4, heatmapCalculator.calculateIntensityLevel(8, 10));
    }

    @Test
    @DisplayName("Single habit heatmap correctly flags scheduled vs unscheduled days")
    void shouldFlagUnscheduledDaysForWeekdayOnlyHabit() {
        Habit weekdayHabit = Habit.builder()
                .id(1L)
                .frequencyType(FrequencyType.WEEKDAYS)
                .build();

        // Friday 2026-03-13 to Sunday 2026-03-15
        LocalDate friday = LocalDate.of(2026, 3, 13);
        LocalDate saturday = LocalDate.of(2026, 3, 14);
        LocalDate sunday = LocalDate.of(2026, 3, 15);

        List<HabitLog> logs = List.of(
                HabitLog.builder().habitId(1L).logDate(friday).completed(true).build()
        );

        HeatmapResponseDto response = heatmapCalculator.buildHabitHeatmap(weekdayHabit, logs, friday, sunday);

        assertNotNull(response);
        assertEquals(3, response.getDays().size());

        HeatmapDayDto friDay = response.getDays().get(0);
        assertTrue(friDay.isScheduled());
        assertEquals(1, friDay.getCompletedCount());
        assertEquals(4, friDay.getLevel());

        HeatmapDayDto satDay = response.getDays().get(1);
        assertFalse(satDay.isScheduled());
        assertEquals(0, satDay.getScheduledCount());
        assertEquals(0, satDay.getLevel());

        HeatmapDayDto sunDay = response.getDays().get(2);
        assertFalse(sunDay.isScheduled());
        assertEquals(0, sunDay.getScheduledCount());
        assertEquals(0, sunDay.getLevel());
    }

    @Test
    @DisplayName("Dashboard heatmap aggregates multiple habits per date")
    void shouldAggregateMultipleHabitsForDashboardHeatmap() {
        Habit habit1 = Habit.builder().id(1L).frequencyType(FrequencyType.DAILY).build();
        Habit habit2 = Habit.builder().id(2L).frequencyType(FrequencyType.DAILY).build();

        LocalDate date = LocalDate.of(2026, 3, 15);
        List<HabitLog> logs = List.of(
                HabitLog.builder().habitId(1L).logDate(date).completed(true).build()
        );

        HeatmapResponseDto response = heatmapCalculator.buildDashboardHeatmap(
                List.of(habit1, habit2), logs, date, date
        );

        assertEquals(1, response.getDays().size());
        HeatmapDayDto day = response.getDays().get(0);
        assertEquals(2, day.getScheduledCount());
        assertEquals(1, day.getCompletedCount());
        assertEquals(2, day.getLevel()); // 1 of 2 = 50% -> Level 2
    }
}
