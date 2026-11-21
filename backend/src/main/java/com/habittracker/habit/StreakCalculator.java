package com.habittracker.habit;

import com.habittracker.habit.dto.HabitStatsDto;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class StreakCalculator {

    public boolean isScheduled(Habit habit, LocalDate date) {
        if (habit.getFrequencyType() == null) {
            return true;
        }

        return switch (habit.getFrequencyType()) {
            case DAILY -> true;
            case WEEKDAYS -> date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
            case SPECIFIC_DAYS -> isSpecificDay(habit.getDaysOfWeek(), date.getDayOfWeek());
        };
    }

    private boolean isSpecificDay(String daysOfWeekStr, DayOfWeek dayOfWeek) {
        if (daysOfWeekStr == null || daysOfWeekStr.trim().isEmpty()) {
            return false;
        }
        String dayName = dayOfWeek.name(); // e.g., "MONDAY"
        String shortDay = dayName.substring(0, 3); // e.g., "MON"

        Set<String> days = Arrays.stream(daysOfWeekStr.toUpperCase().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        return days.contains(dayName) || days.contains(shortDay);
    }

    public HabitStatsDto calculateStats(Habit habit, List<HabitLog> logs, LocalDate today, LocalDate createdDate) {
        Map<LocalDate, Boolean> completionMap = new HashMap<>();
        if (logs != null) {
            for (HabitLog log : logs) {
                completionMap.put(log.getLogDate(), log.isCompleted());
            }
        }

        LocalDate startDate = createdDate != null && !createdDate.isAfter(today) ? createdDate : today;

        // 1. Calculate Current Streak
        int currentStreak = calculateCurrentStreak(habit, completionMap, today, startDate);

        // 2. Calculate Longest Streak, Total Scheduled, and Total Completed
        int longestStreak = 0;
        int runningStreak = 0;
        int totalScheduledDays = 0;
        int totalCompletedDays = 0;

        LocalDate cursor = startDate;
        while (!cursor.isAfter(today)) {
            if (isScheduled(habit, cursor)) {
                totalScheduledDays++;
                boolean isCompleted = completionMap.getOrDefault(cursor, false);
                if (isCompleted) {
                    totalCompletedDays++;
                    runningStreak++;
                    if (runningStreak > longestStreak) {
                        longestStreak = runningStreak;
                    }
                } else {
                    runningStreak = 0;
                }
            }
            cursor = cursor.plusDays(1);
        }

        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }

        double completionRate = totalScheduledDays > 0
                ? Math.round(((double) totalCompletedDays / totalScheduledDays) * 1000.0) / 10.0
                : 0.0;

        return HabitStatsDto.builder()
                .habitId(habit.getId())
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .completionRate(completionRate)
                .totalCompletedDays(totalCompletedDays)
                .totalScheduledDays(totalScheduledDays)
                .build();
    }

    private int calculateCurrentStreak(
            Habit habit, Map<LocalDate, Boolean> completionMap, LocalDate today, LocalDate startDate) {

        int streak = 0;
        LocalDate cursor = today;

        // Check if today was scheduled and completed
        boolean todayScheduled = isScheduled(habit, today);
        boolean todayCompleted = todayScheduled && completionMap.getOrDefault(today, false);

        if (todayCompleted) {
            streak++;
            cursor = previousScheduledDay(habit, today.minusDays(1), startDate);
        } else {
            // Today is either not scheduled yet or not completed yet
            // Streak is preserved if the previous scheduled day was completed
            cursor = previousScheduledDay(habit, today.minusDays(1), startDate);
            if (cursor == null || !completionMap.getOrDefault(cursor, false)) {
                return 0; // Previous scheduled day was missed -> streak is 0
            }
        }

        // Count backwards from cursor
        while (cursor != null && !cursor.isBefore(startDate)) {
            if (completionMap.getOrDefault(cursor, false)) {
                streak++;
                cursor = previousScheduledDay(habit, cursor.minusDays(1), startDate);
            } else {
                break;
            }
        }

        return streak;
    }

    private LocalDate previousScheduledDay(Habit habit, LocalDate from, LocalDate stopBefore) {
        LocalDate current = from;
        while (!current.isBefore(stopBefore)) {
            if (isScheduled(habit, current)) {
                return current;
            }
            current = current.minusDays(1);
        }
        return null;
    }
}
