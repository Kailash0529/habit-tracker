package com.habittracker.habit;

import com.habittracker.common.dto.ApiResponse;
import com.habittracker.habit.dto.CheckInRequest;
import com.habittracker.habit.dto.HabitLogDto;
import com.habittracker.habit.dto.HabitStatsDto;
import com.habittracker.habit.dto.HeatmapResponseDto;
import com.habittracker.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/habits/{id}")
@RequiredArgsConstructor
@Tag(name = "Habit Logs & Stats", description = "Check-ins, streak statistics, and contribution heatmap per habit")
public class HabitLogController {

    private final HabitLogService habitLogService;

    @PutMapping("/logs/{date}")
    @Operation(summary = "Daily check-in", description = "Marks a habit done or undone for a specified date")
    public ResponseEntity<ApiResponse<HabitLogDto>> checkIn(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody CheckInRequest request) {
        HabitLogDto log = habitLogService.checkIn(id, date, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Check-in recorded successfully", log));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get habit stats", description = "Calculates current streak, longest streak, and completion rate")
    public ResponseEntity<ApiResponse<HabitStatsDto>> getHabitStats(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) String timezone) {
        ZoneId zoneId = parseZoneId(timezone);
        HabitStatsDto stats = habitLogService.getHabitStats(id, principal.getId(), zoneId);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/heatmap")
    @Operation(summary = "Get habit contribution heatmap", description = "Returns daily consistency matrix for the specified habit over the given date range")
    public ResponseEntity<ApiResponse<HeatmapResponseDto>> getHabitHeatmap(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String timezone) {
        ZoneId zoneId = parseZoneId(timezone);
        HeatmapResponseDto heatmap = habitLogService.getHabitHeatmap(id, principal.getId(), from, to, zoneId);
        return ResponseEntity.ok(ApiResponse.ok(heatmap));
    }

    private ZoneId parseZoneId(String tz) {
        if (tz == null || tz.trim().isEmpty()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(tz);
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }
}
