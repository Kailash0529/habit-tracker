package com.habittracker.habit;

import com.habittracker.auth.dto.RegisterRequest;
import com.habittracker.habit.dto.CheckInRequest;
import com.habittracker.habit.dto.CreateHabitRequest;
import com.habittracker.user.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HeatmapControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private HabitLogRepository habitLogRepository;

    private String userToken;
    private String otherToken;
    private Integer habitId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        habitLogRepository.deleteAll();
        habitRepository.deleteAll();
        userRepository.deleteAll();

        // Register main user
        RegisterRequest userRequest = RegisterRequest.builder()
                .name("Alex Heatmap")
                .email("alex.heatmap@example.com")
                .password("password123")
                .build();

        userToken = given()
                .contentType(ContentType.JSON)
                .body(userRequest)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .extract()
                .path("data.accessToken");

        // Register other user
        RegisterRequest otherRequest = RegisterRequest.builder()
                .name("Other User")
                .email("other.heatmap@example.com")
                .password("password123")
                .build();

        otherToken = given()
                .contentType(ContentType.JSON)
                .body(otherRequest)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .extract()
                .path("data.accessToken");

        // Create habit
        CreateHabitRequest habitRequest = CreateHabitRequest.builder()
                .name("Guitar Practice")
                .frequencyType(FrequencyType.DAILY)
                .color("#8B5CF6")
                .build();

        habitId = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(habitRequest)
                .when()
                .post("/api/v1/habits")
                .then()
                .statusCode(201)
                .extract()
                .path("data.id");

        // Check in today
        String todayStr = LocalDate.now().toString();
        CheckInRequest checkInRequest = CheckInRequest.builder()
                .completed(true)
                .build();

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(checkInRequest)
                .when()
                .put("/api/v1/habits/" + habitId + "/logs/" + todayStr)
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("GET /api/v1/habits/{id}/heatmap returns valid 52-week default range")
    void shouldReturnHabitHeatmap() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/habits/" + habitId + "/heatmap")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.startDate", notNullValue())
                .body("data.endDate", notNullValue())
                .body("data.totalCompleted", greaterThanOrEqualTo(1))
                .body("data.days", not(empty()));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/heatmap returns aggregated consistency matrix")
    void shouldReturnDashboardHeatmap() {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);

        given()
                .header("Authorization", "Bearer " + userToken)
                .queryParam("from", weekAgo.toString())
                .queryParam("to", today.toString())
                .when()
                .get("/api/v1/dashboard/heatmap")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.days", hasSize(7))
                .body("data.totalCompleted", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("Heatmap returns 400 when from date is after to date")
    void shouldFailWhenFromDateAfterToDate() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        given()
                .header("Authorization", "Bearer " + userToken)
                .queryParam("from", tomorrow.toString())
                .queryParam("to", today.toString())
                .when()
                .get("/api/v1/dashboard/heatmap")
                .then()
                .statusCode(400)
                .body("error", equalTo("Bad Request"))
                .body("message", containsString("'from' date cannot be after 'to' date"));
    }

    @Test
    @DisplayName("User isolation: other user cannot request heatmap for main user's habit")
    void shouldEnforceUserIsolationOnHabitHeatmap() {
        given()
                .header("Authorization", "Bearer " + otherToken)
                .when()
                .get("/api/v1/habits/" + habitId + "/heatmap")
                .then()
                .statusCode(404);
    }
}
