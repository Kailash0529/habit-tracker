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
public class HeatmapResponseDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalCompleted;
    private int totalScheduled;
    private List<HeatmapDayDto> days;
}
