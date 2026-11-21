package com.habittracker.auth;

import com.habittracker.auth.dto.AuthResponse;
import com.habittracker.auth.dto.LoginRequest;
import com.habittracker.auth.dto.RegisterRequest;
import com.habittracker.security.CustomUserDetailsService;
import com.habittracker.security.JwtService;
import com.habittracker.user.User;
import com.habittracker.user.UserMapper;
import com.habittracker.user.UserRepository;
import com.habittracker.user.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private UserDto sampleUserDto;
    private UserDetails sampleUserDetails;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("alex@example.com")
                .name("Alex Smith")
                .passwordHash("encodedPassword")
                .createdAt(Instant.now())
                .build();

        sampleUserDto = UserDto.builder()
                .id(1L)
                .email("alex@example.com")
                .name("Alex Smith")
                .createdAt(sampleUser.getCreatedAt())
                .build();

        sampleUserDetails = new org.springframework.security.core.userdetails.User(
                "alex@example.com", "encodedPassword", Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Successful registration creates user and returns tokens")
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = RegisterRequest.builder()
                .email("alex@example.com")
                .name("Alex Smith")
                .password("plainPassword")
                .build();

        when(userRepository.existsByEmail("alex@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(userDetailsService.loadUserByUsername("alex@example.com")).thenReturn(sampleUserDetails);
        when(jwtService.generateAccessToken(sampleUserDetails, 1L)).thenReturn("access-token-xyz");
        when(jwtService.generateRefreshToken(sampleUserDetails, 1L)).thenReturn("refresh-token-xyz");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);
        when(userMapper.toDto(sampleUser)).thenReturn(sampleUserDto);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access-token-xyz", response.getAccessToken());
        assertEquals("refresh-token-xyz", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("alex@example.com", response.getUser().getEmail());

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Registration throws exception when email already exists")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("alex@example.com")
                .name("Alex Smith")
                .password("plainPassword")
                .build();

        when(userRepository.existsByEmail("alex@example.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Successful login returns tokens and user info")
    void shouldLoginSuccessfully() {
        LoginRequest request = LoginRequest.builder()
                .email("alex@example.com")
                .password("plainPassword")
                .build();

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(sampleUser));
        when(userDetailsService.loadUserByUsername("alex@example.com")).thenReturn(sampleUserDetails);
        when(jwtService.generateAccessToken(sampleUserDetails, 1L)).thenReturn("access-token-xyz");
        when(jwtService.generateRefreshToken(sampleUserDetails, 1L)).thenReturn("refresh-token-xyz");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);
        when(userMapper.toDto(sampleUser)).thenReturn(sampleUserDto);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token-xyz", response.getAccessToken());
        assertEquals("alex@example.com", response.getUser().getEmail());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Login throws BadCredentialsException on invalid credentials")
    void shouldThrowExceptionOnBadCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("alex@example.com")
                .password("wrongPassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
