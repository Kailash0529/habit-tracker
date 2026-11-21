package com.habittracker.habit.dto;

import com.habittracker.habit.FrequencyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHabitRequest {

    @NotBlank(message = "Habit name is required")
    @Size(max = 255, message = "Habit name cannot exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Frequency type is required (DAILY, WEEKDAYS, or SPECIFIC_DAYS)")
    private FrequencyType frequencyType;

    private String daysOfWeek; // e.g., "MON,WED,FRI"

    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Target time must be in HH:mm 24-hour format (e.g. 08:30)")
    private String targetTime;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Color must be a valid hex color code (e.g. #10B981)")
    private String color;
}
