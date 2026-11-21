package com.habittracker.habit;

import com.habittracker.common.dto.ApiResponse;
import com.habittracker.habit.dto.DashboardSummaryDto;
import com.habittracker.habit.dto.HeatmapResponseDto;
import com.habittracker.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard summary and streak analytics")
public class DashboardController {

    private final HabitLogService habitLogService;

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary", description = "Retrieves today's habit statuses, streak counts, and weekly overview")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboardSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String timezone) {
        ZoneId zoneId = parseZoneId(timezone);
        DashboardSummaryDto summary = habitLogService.getDashboardSummary(principal.getId(), zoneId);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/heatmap")
    @Operation(summary = "Get overall contribution heatmap", description = "Returns aggregated GitHub-style habit completion matrix across all habits")
    public ResponseEntity<ApiResponse<HeatmapResponseDto>> getDashboardHeatmap(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String timezone) {
        ZoneId zoneId = parseZoneId(timezone);
        HeatmapResponseDto heatmap = habitLogService.getDashboardHeatmap(principal.getId(), from, to, zoneId);
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
