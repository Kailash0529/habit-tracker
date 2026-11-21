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
public class HabitLogDto {
    private Long id;
    private Long habitId;
    private LocalDate logDate;
    private boolean completed;
    private String note;
}
