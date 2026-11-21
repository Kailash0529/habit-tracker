package com.habittracker.auth;

import com.habittracker.auth.dto.LoginRequest;
import com.habittracker.auth.dto.RegisterRequest;
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
class AuthControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register returns 201 Created and tokens")
    void shouldRegisterSuccessfully() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .password("secret123")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/auth/register")
        .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.accessToken", notNullValue())
                .body("data.refreshToken", notNullValue())
                .body("data.tokenType", equalTo("Bearer"))
                .body("data.user.email", equalTo("jane@example.com"))
                .body("data.user.name", equalTo("Jane Doe"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register returns 400 for duplicate email")
    void shouldFailRegisterForDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .password("secret123")
                .build();

        // First registration
        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/auth/register")
        .then()
                .statusCode(201);

        // Duplicate registration
        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/auth/register")
        .then()
                .statusCode(400)
                .body("error", equalTo("Bad Request"))
                .body("message", containsString("already exists"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register returns 400 for invalid email format")
    void shouldFailRegisterForInvalidEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Jane Doe")
                .email("invalid-email")
                .password("secret123")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/auth/register")
        .then()
                .statusCode(400)
                .body("error", equalTo("Bad Request"))
                .body("validationErrors.email", notNullValue());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 200 OK with valid credentials")
    void shouldLoginSuccessfully() {
        // First register
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .password("secret123")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(registerRequest)
        .when()
                .post("/api/v1/auth/register")
        .then()
                .statusCode(201);

        // Then login
        LoginRequest loginRequest = LoginRequest.builder()
                .email("jane@example.com")
                .password("secret123")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
        .when()
                .post("/api/v1/auth/login")
        .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.accessToken", notNullValue())
                .body("data.user.email", equalTo("jane@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 401 Unauthorized for wrong password")
    void shouldFailLoginWithWrongPassword() {
        // First register
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .password("secret123")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(registerRequest)
        .when()
                .post("/api/v1/auth/register")
        .then()
                .statusCode(201);

        // Then attempt login with wrong password
        LoginRequest loginRequest = LoginRequest.builder()
                .email("jane@example.com")
                .password("wrongpassword")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
        .when()
                .post("/api/v1/auth/login")
        .then()
                .statusCode(401)
                .body("error", equalTo("Unauthorized"))
                .body("message", equalTo("Invalid email or password"));
    }
}
