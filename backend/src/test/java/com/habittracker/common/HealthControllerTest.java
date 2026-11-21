package com.habittracker.common;

import com.habittracker.common.dto.ApiResponse;
import com.habittracker.common.dto.HealthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class HealthControllerTest {

    private final HealthController healthController = new HealthController();

    @Test
    @DisplayName("Health endpoint returns status UP and valid application name")
    void shouldReturnHealthyStatus() {
        ResponseEntity<ApiResponse<HealthResponse>> response = healthController.checkHealth();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ApiResponse<HealthResponse> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());

        HealthResponse healthData = body.getData();
        assertNotNull(healthData);
        assertEquals("UP", healthData.getStatus());
        assertEquals("habit-tracker-backend", healthData.getApplication());
        assertEquals("1.0.0", healthData.getVersion());
        assertNotNull(healthData.getTimestamp());
    }
}
