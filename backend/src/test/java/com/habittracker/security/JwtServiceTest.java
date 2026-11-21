package com.habittracker.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secretKey = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secretKey, 3600000L, 86400000L);
        userDetails = new User("test@example.com", "password", Collections.emptyList());
    }

    @Test
    @DisplayName("Generate access token and extract correct username and userId")
    void shouldGenerateAccessTokenAndExtractClaims() {
        String token = jwtService.generateAccessToken(userDetails, 42L);

        assertNotNull(token);
        assertEquals("test@example.com", jwtService.extractUsername(token));
        assertEquals(42L, jwtService.extractUserId(token));
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Token validation fails when username does not match")
    void shouldFailValidationForDifferentUser() {
        String token = jwtService.generateAccessToken(userDetails, 42L);
        UserDetails anotherUser = new User("other@example.com", "password", Collections.emptyList());

        assertFalse(jwtService.isTokenValid(token, anotherUser));
    }

    @Test
    @DisplayName("Token expiration validation works for expired tokens")
    void shouldDetectExpiredToken() {
        JwtService shortLivedJwtService = new JwtService(secretKey, -1000L, -500L);
        String expiredToken = shortLivedJwtService.generateAccessToken(userDetails, 1L);

        assertFalse(shortLivedJwtService.isTokenValid(expiredToken, userDetails));
    }
}
