package com.habittracker.habit;

import com.habittracker.habit.dto.CreateHabitRequest;
import com.habittracker.habit.dto.HabitDto;
import com.habittracker.habit.dto.UpdateHabitRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class HabitMapper {

    public HabitDto toDto(Habit habit) {
        if (habit == null) {
            return null;
        }
        return HabitDto.builder()
                .id(habit.getId())
                .userId(habit.getUserId())
                .name(habit.getName())
                .description(habit.getDescription())
                .frequencyType(habit.getFrequencyType())
                .daysOfWeek(habit.getDaysOfWeek())
                .targetTime(habit.getTargetTime())
                .color(habit.getColor())
                .archived(habit.isArchived())
                .createdAt(habit.getCreatedAt())
                .build();
    }

    public Habit toEntity(CreateHabitRequest request, Long userId) {
        if (request == null) {
            return null;
        }
        return Habit.builder()
                .userId(userId)
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .frequencyType(request.getFrequencyType())
                .daysOfWeek(request.getDaysOfWeek())
                .targetTime(request.getTargetTime())
                .color(request.getColor() != null ? request.getColor() : "#10B981")
                .archived(false)
                .createdAt(Instant.now())
                .build();
    }

    public void updateEntity(Habit habit, UpdateHabitRequest request) {
        if (habit == null || request == null) {
            return;
        }
        habit.setName(request.getName().trim());
        habit.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        habit.setFrequencyType(request.getFrequencyType());
        habit.setDaysOfWeek(request.getDaysOfWeek());
        habit.setTargetTime(request.getTargetTime());
        if (request.getColor() != null) {
            habit.setColor(request.getColor());
        }
        if (request.getArchived() != null) {
            habit.setArchived(request.getArchived());
        }
    }
}
