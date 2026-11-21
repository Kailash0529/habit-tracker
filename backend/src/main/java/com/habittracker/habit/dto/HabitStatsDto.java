package com.habittracker.habit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitStatsDto {
    private Long habitId;
    private int currentStreak;
    private int longestStreak;
    private double completionRate;
    private int totalCompletedDays;
    private int totalScheduledDays;
}
