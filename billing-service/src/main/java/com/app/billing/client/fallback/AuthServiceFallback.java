package com.app.billing.client. fallback;

import com.app.billing.client.AuthServiceClient;
import com.app.billing.dto.external.ApiResponseWrapper;
import lombok.extern.slf4j. Slf4j;
import org. springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Fallback class for Auth Service when circuit breaker is open
 * or service is unavailable
 */
@Slf4j
@Component
public class AuthServiceFallback implements AuthServiceClient {

    @Override
    public ApiResponseWrapper<Map<String, Object>> getUserById(String userId) {
        log.warn("FALLBACK: Auth service unavailable - getUserById({})", userId);
        return createFallbackResponse("Auth service unavailable", null);
    }

    @Override
    public ApiResponseWrapper<Map<String, Object>> getUserByUsername(String username) {
        log.warn("FALLBACK: Auth service unavailable - getUserByUsername({})", username);
        return createFallbackResponse("Auth service unavailable", null);
    }

    @Override
    public ApiResponseWrapper<Map<String, Boolean>> validateUser(String userId) {
        log.warn("FALLBACK: Auth service unavailable - validateUser({})", userId);
        Map<String, Boolean> fallbackData = new HashMap<>();
        fallbackData.put("exists", false);
        fallbackData.put("valid", false);
        fallbackData.put("fallback", true);
        return new ApiResponseWrapper<>(false, "Auth service unavailable - fallback response", fallbackData);
    }

    private <T> ApiResponseWrapper<T> createFallbackResponse(String message, T data) {
        return new ApiResponseWrapper<>(false, message + " - fallback response", data);
    }
}