package com.habittracker.habit;

import com.habittracker.common.exception.ResourceNotFoundException;
import com.habittracker.habit.dto.CreateHabitRequest;
import com.habittracker.habit.dto.HabitDto;
import com.habittracker.habit.dto.UpdateHabitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private HabitMapper habitMapper;

    @InjectMocks
    private HabitService habitService;

    private Habit sampleHabit;
    private HabitDto sampleHabitDto;

    @BeforeEach
    void setUp() {
        sampleHabit = Habit.builder()
                .id(10L)
                .userId(1L)
                .name("Morning Run")
                .description("Run 5km every morning")
                .frequencyType(FrequencyType.DAILY)
                .color("#10B981")
                .archived(false)
                .createdAt(Instant.now())
                .build();

        sampleHabitDto = HabitDto.builder()
                .id(10L)
                .userId(1L)
                .name("Morning Run")
                .description("Run 5km every morning")
                .frequencyType(FrequencyType.DAILY)
                .color("#10B981")
                .archived(false)
                .createdAt(sampleHabit.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("Create habit successfully saves and returns DTO")
    void shouldCreateHabitSuccessfully() {
        CreateHabitRequest request = CreateHabitRequest.builder()
                .name("Morning Run")
                .frequencyType(FrequencyType.DAILY)
                .color("#10B981")
                .build();

        when(habitMapper.toEntity(request, 1L)).thenReturn(sampleHabit);
        when(habitRepository.save(sampleHabit)).thenReturn(sampleHabit);
        when(habitMapper.toDto(sampleHabit)).thenReturn(sampleHabitDto);

        HabitDto result = habitService.createHabit(request, 1L);

        assertNotNull(result);
        assertEquals("Morning Run", result.getName());
        verify(habitRepository).save(sampleHabit);
    }

    @Test
    @DisplayName("Create habit throws exception when SPECIFIC_DAYS has empty daysOfWeek")
    void shouldThrowExceptionWhenSpecificDaysMissingDaysOfWeek() {
        CreateHabitRequest request = CreateHabitRequest.builder()
                .name("Workout")
                .frequencyType(FrequencyType.SPECIFIC_DAYS)
                .daysOfWeek("   ")
                .build();

        assertThrows(IllegalArgumentException.class, () -> habitService.createHabit(request, 1L));
        verify(habitRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get habit by ID succeeds when owned by user")
    void shouldGetHabitByIdWhenOwnedByUser() {
        when(habitRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleHabit));
        when(habitMapper.toDto(sampleHabit)).thenReturn(sampleHabitDto);

        HabitDto result = habitService.getHabitById(10L, 1L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    @DisplayName("Get habit by ID throws ResourceNotFoundException when user is not owner")
    void shouldThrowExceptionWhenHabitNotFoundOrNotOwned() {
        when(habitRepository.findByIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> habitService.getHabitById(10L, 2L));
    }

    @Test
    @DisplayName("Update habit modifies entity and saves")
    void shouldUpdateHabitSuccessfully() {
        UpdateHabitRequest request = UpdateHabitRequest.builder()
                .name("Evening Run")
                .frequencyType(FrequencyType.DAILY)
                .color("#3B82F6")
                .build();

        when(habitRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleHabit));
        doAnswer(invocation -> {
            sampleHabit.setName("Evening Run");
            sampleHabit.setColor("#3B82F6");
            return null;
        }).when(habitMapper).updateEntity(sampleHabit, request);

        when(habitRepository.save(sampleHabit)).thenReturn(sampleHabit);
        when(habitMapper.toDto(sampleHabit)).thenReturn(sampleHabitDto);

        HabitDto result = habitService.updateHabit(10L, request, 1L);

        assertNotNull(result);
        verify(habitRepository).save(sampleHabit);
    }

    @Test
    @DisplayName("Delete habit deletes when owned by user")
    void shouldDeleteHabitSuccessfully() {
        when(habitRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleHabit));

        habitService.deleteHabit(10L, 1L);

        verify(habitRepository).delete(sampleHabit);
    }

    @Test
    @DisplayName("Delete habit throws ResourceNotFoundException when not owned by user")
    void shouldFailDeleteWhenNotOwned() {
        when(habitRepository.findByIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> habitService.deleteHabit(10L, 2L));
        verify(habitRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Archive habit sets archived flag")
    void shouldArchiveHabit() {
        when(habitRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleHabit));
        when(habitRepository.save(sampleHabit)).thenReturn(sampleHabit);
        when(habitMapper.toDto(sampleHabit)).thenReturn(sampleHabitDto);

        HabitDto result = habitService.archiveHabit(10L, 1L, true);

        assertNotNull(result);
        assertTrue(sampleHabit.isArchived());
    }
}
