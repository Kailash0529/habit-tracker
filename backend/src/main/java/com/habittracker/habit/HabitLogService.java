package com.habittracker.habit;

import com.habittracker.common.exception.ResourceNotFoundException;
import com.habittracker.habit.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HabitLogService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final HabitLogMapper habitLogMapper;
    private final HabitMapper habitMapper;
    private final StreakCalculator streakCalculator;
    private final HeatmapCalculator heatmapCalculator;

    @Transactional
    public HabitLogDto checkIn(Long habitId, LocalDate date, CheckInRequest request, Long userId) {
        Habit habit = verifyHabitOwnership(habitId, userId);

        HabitLog log = habitLogRepository.findByHabitIdAndLogDate(habit.getId(), date)
                .orElseGet(() -> HabitLog.builder()
                        .habitId(habit.getId())
                        .logDate(date)
                        .build());

        log.setCompleted(Boolean.TRUE.equals(request.getCompleted()));
        if (request.getNote() != null) {
            log.setNote(request.getNote().trim());
        }

        HabitLog savedLog = habitLogRepository.save(log);
        return habitLogMapper.toDto(savedLog);
    }

    @Transactional(readOnly = true)
    public HabitStatsDto getHabitStats(Long habitId, Long userId, ZoneId zoneId) {
        Habit habit = verifyHabitOwnership(habitId, userId);
        LocalDate today = LocalDate.now(zoneId != null ? zoneId : ZoneId.systemDefault());
        LocalDate createdDate = habit.getCreatedAt().atZone(zoneId != null ? zoneId : ZoneId.systemDefault()).toLocalDate();

        List<HabitLog> logs = habitLogRepository.findAllByHabitIdOrderByLogDateAsc(habit.getId());
        return streakCalculator.calculateStats(habit, logs, today, createdDate);
    }

    @Transactional(readOnly = true)
    public HeatmapResponseDto getHabitHeatmap(
            Long habitId, Long userId, LocalDate from, LocalDate to, ZoneId zoneId) {

        Habit habit = verifyHabitOwnership(habitId, userId);
        LocalDate[] range = resolveHeatmapRange(from, to, zoneId);
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        List<HabitLog> logs = habitLogRepository.findAllByHabitIdAndLogDateBetweenOrderByLogDateAsc(
                habit.getId(), startDate, endDate);

        return heatmapCalculator.buildHabitHeatmap(habit, logs, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public HeatmapResponseDto getDashboardHeatmap(
            Long userId, LocalDate from, LocalDate to, ZoneId zoneId) {

        LocalDate[] range = resolveHeatmapRange(from, to, zoneId);
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        List<Habit> habits = habitRepository.findAllByUserIdAndArchivedOrderByCreatedAtDesc(userId, false);
        if (habits.isEmpty()) {
            return HeatmapResponseDto.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalCompleted(0)
                    .totalScheduled(0)
                    .days(Collections.emptyList())
                    .build();
        }

        List<Long> habitIds = habits.stream().map(Habit::getId).toList();
        List<HabitLog> logs = habitLogRepository.findAllByHabitIdInAndLogDateBetween(
                habitIds, startDate, endDate);

        return heatmapCalculator.buildDashboardHeatmap(habits, logs, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary(Long userId, ZoneId zoneId) {
        LocalDate today = LocalDate.now(zoneId != null ? zoneId : ZoneId.systemDefault());

        List<Habit> habits = habitRepository.findAllByUserIdAndArchivedOrderByCreatedAtDesc(userId, false);
        if (habits.isEmpty()) {
            return DashboardSummaryDto.builder()
                    .todayDate(today)
                    .todayCompletedCount(0)
                    .todayScheduledCount(0)
                    .overallCompletionRate(0.0)
                    .todayHabits(Collections.emptyList())
                    .weeklyOverview(buildEmptyWeeklyOverview(today))
                    .build();
        }

        List<Long> habitIds = habits.stream().map(Habit::getId).toList();

        // 1. Fetch Today's Logs
        List<HabitLog> todayLogs = habitLogRepository.findAllByHabitIdInAndLogDate(habitIds, today);
        Map<Long, HabitLog> todayLogMap = todayLogs.stream()
                .collect(Collectors.toMap(HabitLog::getHabitId, log -> log));

        // 2. Fetch past 7 days logs for weekly overview
        LocalDate weekStart = today.minusDays(6);
        List<HabitLog> weekLogs = habitLogRepository.findAllByHabitIdInAndLogDateBetween(habitIds, weekStart, today);
        Map<String, Boolean> weekCompletionMap = new HashMap<>();
        for (HabitLog l : weekLogs) {
            weekCompletionMap.put(l.getHabitId() + "_" + l.getLogDate(), l.isCompleted());
        }

        // 3. Build Today's Habit summaries
        int todayScheduledCount = 0;
        int todayCompletedCount = 0;
        List<DashboardSummaryDto.TodayHabitDto> todayHabitDtos = new ArrayList<>();

        for (Habit habit : habits) {
            boolean isScheduledToday = streakCalculator.isScheduled(habit, today);
            HabitLog todayLog = todayLogMap.get(habit.getId());
            boolean isCompletedToday = todayLog != null && todayLog.isCompleted();
            String noteToday = todayLog != null ? todayLog.getNote() : null;

            if (isScheduledToday) {
                todayScheduledCount++;
                if (isCompletedToday) {
                    todayCompletedCount++;
                }
            }

            LocalDate habitCreatedDate = habit.getCreatedAt().atZone(zoneId != null ? zoneId : ZoneId.systemDefault()).toLocalDate();
            List<HabitLog> habitLogs = habitLogRepository.findAllByHabitIdOrderByLogDateAsc(habit.getId());
            HabitStatsDto stats = streakCalculator.calculateStats(habit, habitLogs, today, habitCreatedDate);

            todayHabitDtos.add(DashboardSummaryDto.TodayHabitDto.builder()
                    .habit(habitMapper.toDto(habit))
                    .completedToday(isCompletedToday)
                    .noteToday(noteToday)
                    .currentStreak(stats.getCurrentStreak())
                    .build());
        }

        // 4. Build Weekly Overview (last 7 days ending today)
        List<DashboardSummaryDto.DaySummaryDto> weeklyOverview = new ArrayList<>();
        LocalDate dayCursor = weekStart;
        while (!dayCursor.isAfter(today)) {
            int scheduledOnDay = 0;
            int completedOnDay = 0;

            for (Habit habit : habits) {
                if (streakCalculator.isScheduled(habit, dayCursor)) {
                    scheduledOnDay++;
                    if (Boolean.TRUE.equals(weekCompletionMap.get(habit.getId() + "_" + dayCursor))) {
                        completedOnDay++;
                    }
                }
            }

            weeklyOverview.add(DashboardSummaryDto.DaySummaryDto.builder()
                    .date(dayCursor)
                    .dayOfWeek(dayCursor.getDayOfWeek().name().substring(0, 3))
                    .scheduledCount(scheduledOnDay)
                    .completedCount(completedOnDay)
                    .build());

            dayCursor = dayCursor.plusDays(1);
        }

        double overallCompletionRate = todayScheduledCount > 0
                ? Math.round(((double) todayCompletedCount / todayScheduledCount) * 1000.0) / 10.0
                : 0.0;

        return DashboardSummaryDto.builder()
                .todayDate(today)
                .todayCompletedCount(todayCompletedCount)
                .todayScheduledCount(todayScheduledCount)
                .overallCompletionRate(overallCompletionRate)
                .todayHabits(todayHabitDtos)
                .weeklyOverview(weeklyOverview)
                .build();
    }

    private Habit verifyHabitOwnership(Long habitId, Long userId) {
        return habitRepository.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + habitId));
    }

    private LocalDate[] resolveHeatmapRange(LocalDate from, LocalDate to, ZoneId zoneId) {
        LocalDate today = LocalDate.now(zoneId != null ? zoneId : ZoneId.systemDefault());
        LocalDate endDate = to != null ? to : today;
        LocalDate startDate = from != null ? from : endDate.minusWeeks(52).plusDays(1);

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("'from' date cannot be after 'to' date");
        }

        return new LocalDate[]{startDate, endDate};
    }

    private List<DashboardSummaryDto.DaySummaryDto> buildEmptyWeeklyOverview(LocalDate today) {
        List<DashboardSummaryDto.DaySummaryDto> list = new ArrayList<>();
        LocalDate cursor = today.minusDays(6);
        while (!cursor.isAfter(today)) {
            list.add(DashboardSummaryDto.DaySummaryDto.builder()
                    .date(cursor)
                    .dayOfWeek(cursor.getDayOfWeek().name().substring(0, 3))
                    .completedCount(0)
                    .scheduledCount(0)
                    .build());
            cursor = cursor.plusDays(1);
        }
        return list;
    }
}
