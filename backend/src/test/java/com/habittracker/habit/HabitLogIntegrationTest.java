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
class HabitLogIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private HabitLogRepository habitLogRepository;

    private String userToken;
    private String otherUserToken;
    private Integer habitId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        habitLogRepository.deleteAll();
        habitRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Register main user
        RegisterRequest userRequest = RegisterRequest.builder()
                .name("Sam Runner")
                .email("sam@example.com")
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

        // 2. Register other user
        RegisterRequest otherRequest = RegisterRequest.builder()
                .name("Other User")
                .email("other@example.com")
                .password("password123")
                .build();

        otherUserToken = given()
                .contentType(ContentType.JSON)
                .body(otherRequest)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .extract()
                .path("data.accessToken");

        // 3. Create a daily habit for main user
        CreateHabitRequest habitRequest = CreateHabitRequest.builder()
                .name("Morning Meditation")
                .description("10 minutes of mindfulness")
                .frequencyType(FrequencyType.DAILY)
                .color("#10B981")
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
    }

    @Test
    @DisplayName("Check in today, verify stats update and dashboard reflects completion")
    void shouldCheckInAndUpdateStatsAndDashboard() {
        String todayStr = LocalDate.now().toString();

        // 1. Perform check-in: mark done
        CheckInRequest checkInRequest = CheckInRequest.builder()
                .completed(true)
                .note("Felt very peaceful")
                .build();

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(checkInRequest)
                .when()
                .put("/api/v1/habits/" + habitId + "/logs/" + todayStr)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.completed", equalTo(true))
                .body("data.note", equalTo("Felt very peaceful"));

        // 2. Fetch stats for the habit
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/habits/" + habitId + "/stats")
                .then()
                .statusCode(200)
                .body("data.currentStreak", greaterThanOrEqualTo(1))
                .body("data.totalCompletedDays", greaterThanOrEqualTo(1));

        // 3. Fetch dashboard summary
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/dashboard/summary")
                .then()
                .statusCode(200)
                .body("data.todayCompletedCount", greaterThanOrEqualTo(1))
                .body("data.todayHabits[0].completedToday", equalTo(true))
                .body("data.todayHabits[0].noteToday", equalTo("Felt very peaceful"));

        // 4. Toggle check-in to undone
        CheckInRequest undoRequest = CheckInRequest.builder()
                .completed(false)
                .build();

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(undoRequest)
                .when()
                .put("/api/v1/habits/" + habitId + "/logs/" + todayStr)
                .then()
                .statusCode(200)
                .body("data.completed", equalTo(false));
    }

    @Test
    @DisplayName("User isolation: other user cannot check in or view stats for main user's habit")
    void shouldEnforceUserIsolationOnLogsAndStats() {
        String todayStr = LocalDate.now().toString();
        CheckInRequest checkInRequest = CheckInRequest.builder()
                .completed(true)
                .build();

        // Other user attempts check in -> 404 Not Found
        given()
                .header("Authorization", "Bearer " + otherUserToken)
                .contentType(ContentType.JSON)
                .body(checkInRequest)
                .when()
                .put("/api/v1/habits/" + habitId + "/logs/" + todayStr)
                .then()
                .statusCode(404);

        // Other user attempts to get stats -> 404 Not Found
        given()
                .header("Authorization", "Bearer " + otherUserToken)
                .when()
                .get("/api/v1/habits/" + habitId + "/stats")
                .then()
                .statusCode(404);
    }
}
