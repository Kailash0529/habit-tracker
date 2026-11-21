package com.habittracker.habit;

import com.habittracker.habit.dto.HeatmapDayDto;
import com.habittracker.habit.dto.HeatmapResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
public class HeatmapCalculator {

    private final StreakCalculator streakCalculator;

    public int calculateIntensityLevel(int completedCount, int scheduledCount) {
        if (scheduledCount <= 0 || completedCount <= 0) {
            return 0;
        }

        double percentage = ((double) completedCount / scheduledCount) * 100.0;

        if (percentage <= 25.0) {
            return 1;
        } else if (percentage <= 50.0) {
            return 2;
        } else if (percentage <= 75.0) {
            return 3;
        } else {
            return 4;
        }
    }

    public HeatmapResponseDto buildHabitHeatmap(
            Habit habit, List<HabitLog> logs, LocalDate from, LocalDate to) {

        Map<LocalDate, Boolean> completionMap = new HashMap<>();
        if (logs != null) {
            for (HabitLog log : logs) {
                completionMap.put(log.getLogDate(), log.isCompleted());
            }
        }

        List<HeatmapDayDto> dayDtos = new ArrayList<>();
        int totalCompleted = 0;
        int totalScheduled = 0;

        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            boolean isScheduled = streakCalculator.isScheduled(habit, cursor);
            boolean isCompleted = isScheduled && completionMap.getOrDefault(cursor, false);

            int scheduledCount = isScheduled ? 1 : 0;
            int completedCount = isCompleted ? 1 : 0;

            if (isScheduled) {
                totalScheduled++;
                if (isCompleted) {
                    totalCompleted++;
                }
            }

            int level = isCompleted ? 4 : 0;

            dayDtos.add(HeatmapDayDto.builder()
                    .date(cursor)
                    .scheduledCount(scheduledCount)
                    .completedCount(completedCount)
                    .level(level)
                    .scheduled(isScheduled)
                    .build());

            cursor = cursor.plusDays(1);
        }

        return HeatmapResponseDto.builder()
                .startDate(from)
                .endDate(to)
                .totalCompleted(totalCompleted)
                .totalScheduled(totalScheduled)
                .days(dayDtos)
                .build();
    }

    public HeatmapResponseDto buildDashboardHeatmap(
            List<Habit> habits, List<HabitLog> logs, LocalDate from, LocalDate to) {

        // Index logs by habitId_date for O(1) lookup
        Map<String, Boolean> logMap = new HashMap<>();
        if (logs != null) {
            for (HabitLog log : logs) {
                if (log.isCompleted()) {
                    logMap.put(log.getHabitId() + "_" + log.getLogDate(), true);
                }
            }
        }

        List<HeatmapDayDto> dayDtos = new ArrayList<>();
        int totalCompleted = 0;
        int totalScheduled = 0;

        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            int scheduledCount = 0;
            int completedCount = 0;

            for (Habit habit : habits) {
                if (streakCalculator.isScheduled(habit, cursor)) {
                    scheduledCount++;
                    if (Boolean.TRUE.equals(logMap.get(habit.getId() + "_" + cursor))) {
                        completedCount++;
                    }
                }
            }

            totalScheduled += scheduledCount;
            totalCompleted += completedCount;

            int level = calculateIntensityLevel(completedCount, scheduledCount);

            dayDtos.add(HeatmapDayDto.builder()
                    .date(cursor)
                    .scheduledCount(scheduledCount)
                    .completedCount(completedCount)
                    .level(level)
                    .scheduled(scheduledCount > 0)
                    .build());

            cursor = cursor.plusDays(1);
        }

        return HeatmapResponseDto.builder()
                .startDate(from)
                .endDate(to)
                .totalCompleted(totalCompleted)
                .totalScheduled(totalScheduled)
                .days(dayDtos)
                .build();
    }
}
