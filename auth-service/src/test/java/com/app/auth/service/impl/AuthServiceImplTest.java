package com.app.auth.service.impl;

import com.app.auth.dto.request.LoginRequestDTO;
import com.app.auth.dto.response.AuthResponseDTO;
import com.app.auth.exception.EmailNotVerifiedException;
import com.app.auth.exception.InvalidCredentialsException;
import com.app.auth.model.RefreshToken;
import com.app.auth.model.User;
import com.app.auth.repository.UserRepository;
import com.app.auth.security.JwtUtils;
import com.app.auth.service.AuditLogService;
import com.app.auth.service.EmailService;
import com.app.auth.service.RateLimitService;
import com.app.auth.service.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuditLogService auditLogService;
    @Mock private RateLimitService rateLimitService;
    @Mock private EmailService emailService;

    // =========================
    // LOGIN SUCCESS (EMAIL VERIFIED)
    // =========================
    @Test
    void login_success_whenEmailVerified() {

        User user = User.builder()
                .id("u1")
                .username("john")
                .password("hashed-password")
                .emailVerified(true)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token")
                .userId("u1")
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(any(), any()))
                .thenReturn(true);

        when(jwtUtils.generateAccessToken(user))
                .thenReturn("access-token");

        when(refreshTokenService.createRefreshToken(any(), any(), any()))
                .thenReturn(refreshToken);

        AuthResponseDTO response = authService.login(
                new LoginRequestDTO("john", "Password@123", null),
                mock(HttpServletRequest.class)
        );

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    // =========================
    // LOGIN FAILS – EMAIL NOT VERIFIED
    // =========================
    @Test
    void login_fails_whenEmailNotVerified() {

        User user = User.builder()
                .username("john")
                .password("hashed-password")
                .emailVerified(false) // 🔴 KEY CONDITION
                .enabled(true)
                .accountNonLocked(true)
                .build();

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(any(), any()))
                .thenReturn(true);

        assertThrows(
                EmailNotVerifiedException.class,
                () -> authService.login(
                        new LoginRequestDTO("john", "Password@123", null),
                        mock(HttpServletRequest.class)
                )
        );
    }

    // =========================
    // LOGIN FAILS – WRONG PASSWORD
    // =========================
    @Test
    void login_fails_whenPasswordIsWrong() {

        User user = User.builder()
                .username("john")
                .password("hashed-password")
                .emailVerified(true)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(any(), any()))
                .thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(
                        new LoginRequestDTO("john", "wrong-password", null),
                        mock(HttpServletRequest.class)
                )
        );
    }
}
