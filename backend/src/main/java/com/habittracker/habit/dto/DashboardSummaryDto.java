package com.habittracker.habit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private LocalDate todayDate;
    private int todayCompletedCount;
    private int todayScheduledCount;
    private double overallCompletionRate;
    private List<TodayHabitDto> todayHabits;
    private List<DaySummaryDto> weeklyOverview;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayHabitDto {
        private HabitDto habit;
        private boolean completedToday;
        private String noteToday;
        private int currentStreak;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySummaryDto {
        private LocalDate date;
        private String dayOfWeek;
        private int completedCount;
        private int scheduledCount;
    }
}
