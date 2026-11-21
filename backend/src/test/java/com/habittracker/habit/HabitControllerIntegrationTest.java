package com.habittracker.habit;

import com.habittracker.auth.dto.RegisterRequest;
import com.habittracker.habit.dto.CreateHabitRequest;
import com.habittracker.habit.dto.UpdateHabitRequest;
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HabitControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HabitRepository habitRepository;

    private String userAToken;
    private String userBToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        habitRepository.deleteAll();
        userRepository.deleteAll();

        // Register User A
        RegisterRequest userA = RegisterRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("password123")
                .build();

        userAToken = given()
                .contentType(ContentType.JSON)
                .body(userA)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .extract()
                .path("data.accessToken");

        // Register User B
        RegisterRequest userB = RegisterRequest.builder()
                .name("Bob")
                .email("bob@example.com")
                .password("password123")
                .build();

        userBToken = given()
                .contentType(ContentType.JSON)
                .body(userB)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .extract()
                .path("data.accessToken");
    }

    @Test
    @DisplayName("Create, read, update, archive, and delete habit workflow for authenticated user")
    void shouldCompleteHabitCrudWorkflow() {
        // 1. Create Habit for User A
        CreateHabitRequest createRequest = CreateHabitRequest.builder()
                .name("Read Books")
                .description("Read 20 pages every day")
                .frequencyType(FrequencyType.DAILY)
                .targetTime("21:00")
                .color("#3B82F6")
                .build();

        Integer habitId = given()
                .header("Authorization", "Bearer " + userAToken)
                .contentType(ContentType.JSON)
                .body(createRequest)
                .when()
                .post("/api/v1/habits")
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.name", equalTo("Read Books"))
                .body("data.color", equalTo("#3B82F6"))
                .body("data.archived", equalTo(false))
                .extract()
                .path("data.id");

        // 2. List Habits for User A
        given()
                .header("Authorization", "Bearer " + userAToken)
                .when()
                .get("/api/v1/habits")
                .then()
                .statusCode(200)
                .body("data", hasSize(1))
                .body("data[0].id", equalTo(habitId));

        // 3. Get Habit by ID
        given()
                .header("Authorization", "Bearer " + userAToken)
                .when()
                .get("/api/v1/habits/" + habitId)
                .then()
                .statusCode(200)
                .body("data.id", equalTo(habitId))
                .body("data.name", equalTo("Read Books"));

        // 4. Update Habit
        UpdateHabitRequest updateRequest = UpdateHabitRequest.builder()
                .name("Read Books & Articles")
                .description("Read 30 pages daily")
                .frequencyType(FrequencyType.DAILY)
                .targetTime("22:00")
                .color("#10B981")
                .archived(false)
                .build();

        given()
                .header("Authorization", "Bearer " + userAToken)
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .put("/api/v1/habits/" + habitId)
                .then()
                .statusCode(200)
                .body("data.name", equalTo("Read Books & Articles"))
                .body("data.color", equalTo("#10B981"));

        // 5. Archive Habit
        given()
                .header("Authorization", "Bearer " + userAToken)
                .when()
                .patch("/api/v1/habits/" + habitId + "/archive?archive=true")
                .then()
                .statusCode(200)
                .body("data.archived", equalTo(true));

        // 6. Delete Habit
        given()
                .header("Authorization", "Bearer " + userAToken)
                .when()
                .delete("/api/v1/habits/" + habitId)
                .then()
                .statusCode(200)
                .body("message", equalTo("Habit deleted successfully"));

        // 7. Verify Deleted
        given()
                .header("Authorization", "Bearer " + userAToken)
                .when()
                .get("/api/v1/habits/" + habitId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Strict user isolation: User B cannot view, edit, or delete User A's habit")
    void shouldEnforceStrictUserIsolation() {
        // User A creates a habit
        CreateHabitRequest createRequest = CreateHabitRequest.builder()
                .name("Alice Private Diary")
                .frequencyType(FrequencyType.DAILY)
                .build();

        Integer aliceHabitId = given()
                .header("Authorization", "Bearer " + userAToken)
                .contentType(ContentType.JSON)
                .body(createRequest)
                .when()
                .post("/api/v1/habits")
                .then()
                .statusCode(201)
                .extract()
                .path("data.id");

        // User B lists habits -> should be empty
        given()
                .header("Authorization", "Bearer " + userBToken)
                .when()
                .get("/api/v1/habits")
                .then()
                .statusCode(200)
                .body("data", hasSize(0));

        // User B tries to GET Alice's habit -> 404 NOT FOUND
        given()
                .header("Authorization", "Bearer " + userBToken)
                .when()
                .get("/api/v1/habits/" + aliceHabitId)
                .then()
                .statusCode(404)
                .body("error", equalTo("Not Found"));

        // User B tries to PUT Alice's habit -> 404 NOT FOUND
        UpdateHabitRequest unauthorizedUpdate = UpdateHabitRequest.builder()
                .name("Hacked Name")
                .frequencyType(FrequencyType.DAILY)
                .build();

        given()
                .header("Authorization", "Bearer " + userBToken)
                .contentType(ContentType.JSON)
                .body(unauthorizedUpdate)
                .when()
                .put("/api/v1/habits/" + aliceHabitId)
                .then()
                .statusCode(404);

        // User B tries to DELETE Alice's habit -> 404 NOT FOUND
        given()
                .header("Authorization", "Bearer " + userBToken)
                .when()
                .delete("/api/v1/habits/" + aliceHabitId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Unauthenticated requests to /api/v1/habits return 401 Unauthorized")
    void shouldRejectUnauthenticatedRequests() {
        given()
                .when()
                .get("/api/v1/habits")
                .then()
                .statusCode(401)
                .body("error", equalTo("Unauthorized"))
                .body("status", equalTo(401));
    }
}
