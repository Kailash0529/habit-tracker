package com.habittracker.habit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapDayDto {
    private LocalDate date;
    private int completedCount;
    private int scheduledCount;
    private int level; // 0 (none) to 4 (max)
    private boolean scheduled;
}
