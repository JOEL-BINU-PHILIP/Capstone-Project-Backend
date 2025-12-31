package com.app.auth.service;

public interface RateLimitService {
    void checkLoginRateLimit(String username, String ipAddress);
}
