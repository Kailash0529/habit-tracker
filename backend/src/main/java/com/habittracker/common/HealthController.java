package com.habittracker.common;

import com.habittracker.common.dto.ApiResponse;
import com.habittracker.common.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Application health and status check")
public class HealthController {

    @GetMapping
    @Operation(summary = "Health check endpoint", description = "Returns service availability status and current timestamp")
    public ResponseEntity<ApiResponse<HealthResponse>> checkHealth() {
        HealthResponse health = HealthResponse.builder()
                .status("UP")
                .application("habit-tracker-backend")
                .version("1.0.0")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(health));
    }
}
