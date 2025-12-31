package com.app.auth.service.impl;

import com.app.auth.exception.RateLimitExceededException;
import com.app.auth.repository.LoginAttemptRepository;
import com.app.auth.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final LoginAttemptRepository loginAttemptRepository;

    @Value("${app.security.max-login-attempts-per-username:10}")
    private int maxAttemptsPerUsername;

    @Value("${app.security.max-login-attempts-per-ip:20}")
    private int maxAttemptsPerIp;

    @Value("${app.security.rate-limit-window-minutes:15}")
    private int rateLimitWindowMinutes;

    @Override
    public void checkLoginRateLimit(String username, String ipAddress) {
        Instant windowStart = Instant.now().minusSeconds(rateLimitWindowMinutes * 60);

        // Check username rate limit
        long usernameAttempts = loginAttemptRepository
                .countFailedAttemptsByUsername(username, windowStart);

        if (usernameAttempts >= maxAttemptsPerUsername) {
            throw new RateLimitExceededException(
                    "Too many login attempts for this username. Please try again later.",
                    rateLimitWindowMinutes * 60
            );
        }

        // Check IP rate limit
        long ipAttempts = loginAttemptRepository
                .countFailedAttemptsByIp(ipAddress, windowStart);

        if (ipAttempts >= maxAttemptsPerIp) {
            throw new RateLimitExceededException(
                    "Too many login attempts from this IP address. Please try again later.",
                    rateLimitWindowMinutes * 60
            );
        }
    }
}
