package com.habittracker.habit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRequest {

    @NotNull(message = "Completed status is required (true or false)")
    private Boolean completed;

    @Size(max = 1000, message = "Note cannot exceed 1000 characters")
    private String note;
}
