package com.habittracker.habit;

import com.habittracker.common.exception.ResourceNotFoundException;
import com.habittracker.habit.dto.CreateHabitRequest;
import com.habittracker.habit.dto.HabitDto;
import com.habittracker.habit.dto.UpdateHabitRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitMapper habitMapper;

    @Transactional(readOnly = true)
    public List<HabitDto> getHabits(Long userId, Boolean archived) {
        List<Habit> habits;
        if (archived != null) {
            habits = habitRepository.findAllByUserIdAndArchivedOrderByCreatedAtDesc(userId, archived);
        } else {
            habits = habitRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        }
        return habits.stream()
                .map(habitMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public HabitDto getHabitById(Long id, Long userId) {
        Habit habit = findHabitForUser(id, userId);
        return habitMapper.toDto(habit);
    }

    @Transactional
    public HabitDto createHabit(CreateHabitRequest request, Long userId) {
        validateFrequencyDetails(request.getFrequencyType(), request.getDaysOfWeek());

        Habit habit = habitMapper.toEntity(request, userId);
        Habit savedHabit = habitRepository.save(habit);
        return habitMapper.toDto(savedHabit);
    }

    @Transactional
    public HabitDto updateHabit(Long id, UpdateHabitRequest request, Long userId) {
        validateFrequencyDetails(request.getFrequencyType(), request.getDaysOfWeek());

        Habit habit = findHabitForUser(id, userId);
        habitMapper.updateEntity(habit, request);
        Habit updatedHabit = habitRepository.save(habit);
        return habitMapper.toDto(updatedHabit);
    }

    @Transactional
    public void deleteHabit(Long id, Long userId) {
        Habit habit = findHabitForUser(id, userId);
        habitRepository.delete(habit);
    }

    @Transactional
    public HabitDto archiveHabit(Long id, Long userId, boolean archive) {
        Habit habit = findHabitForUser(id, userId);
        habit.setArchived(archive);
        Habit savedHabit = habitRepository.save(habit);
        return habitMapper.toDto(savedHabit);
    }

    private Habit findHabitForUser(Long id, Long userId) {
        return habitRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + id));
    }

    private void validateFrequencyDetails(FrequencyType frequencyType, String daysOfWeek) {
        if (frequencyType == FrequencyType.SPECIFIC_DAYS && (daysOfWeek == null || daysOfWeek.trim().isEmpty())) {
            throw new IllegalArgumentException("Days of week must be provided when frequency is SPECIFIC_DAYS");
        }
    }
}
