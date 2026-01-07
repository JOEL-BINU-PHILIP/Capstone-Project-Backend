package com.app.auth.service.impl;

import com.app.auth.exception.RateLimitExceededException;
import com.app.auth.repository.LoginAttemptRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceImplTest {

    @InjectMocks
    private RateLimitServiceImpl rateLimitService;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimitService, "maxAttemptsPerUsername", 10);
        ReflectionTestUtils.setField(rateLimitService, "maxAttemptsPerIp", 20);
        ReflectionTestUtils.setField(rateLimitService, "rateLimitWindowMinutes", 15);
    }

    // ==================== CHECK LOGIN RATE LIMIT TESTS ====================

    @Test
    void checkLoginRateLimit_success() {
        when(loginAttemptRepository.countFailedAttemptsByUsername(eq("testuser"), any(Instant.class)))
                .thenReturn(5L);
        when(loginAttemptRepository.countFailedAttemptsByIp(eq("127.0.0.1"), any(Instant.class)))
                .thenReturn(10L);

        // Should not throw any exception
        assertDoesNotThrow(() -> rateLimitService.checkLoginRateLimit("testuser", "127.0.0.1"));
    }

    @Test
    void checkLoginRateLimit_usernameExceeded() {
        when(loginAttemptRepository.countFailedAttemptsByUsername(eq("testuser"), any(Instant.class)))
                .thenReturn(10L);

        RateLimitExceededException exception = assertThrows(RateLimitExceededException.class, () ->
                rateLimitService.checkLoginRateLimit("testuser", "127.0.0.1")
        );

        assertTrue(exception.getMessage().contains("Too many login attempts for this username"));
    }

    @Test
    void checkLoginRateLimit_ipExceeded() {
        when(loginAttemptRepository.countFailedAttemptsByUsername(eq("testuser"), any(Instant.class)))
                .thenReturn(5L);
        when(loginAttemptRepository.countFailedAttemptsByIp(eq("127.0.0.1"), any(Instant.class)))
                .thenReturn(20L);

        RateLimitExceededException exception = assertThrows(RateLimitExceededException.class, () ->
                rateLimitService.checkLoginRateLimit("testuser", "127.0.0.1")
        );

        assertTrue(exception.getMessage().contains("Too many login attempts from this IP address"));
    }

    @Test
    void checkLoginRateLimit_zeroAttempts() {
        when(loginAttemptRepository.countFailedAttemptsByUsername(eq("newuser"), any(Instant.class)))
                .thenReturn(0L);
        when(loginAttemptRepository.countFailedAttemptsByIp(eq("192.168.1.1"), any(Instant.class)))
                .thenReturn(0L);

        assertDoesNotThrow(() -> rateLimitService.checkLoginRateLimit("newuser", "192.168.1.1"));
    }

    @Test
    void checkLoginRateLimit_atLimit() {
        when(loginAttemptRepository.countFailedAttemptsByUsername(eq("testuser"), any(Instant.class)))
                .thenReturn(9L);
        when(loginAttemptRepository.countFailedAttemptsByIp(eq("127.0.0.1"), any(Instant.class)))
                .thenReturn(19L);

        // Should not throw any exception when at limit - 1
        assertDoesNotThrow(() -> rateLimitService.checkLoginRateLimit("testuser", "127.0.0.1"));
    }
}

