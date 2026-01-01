package com.app. booking.client.fallback;

import com.app.booking.client.AuthServiceClient;
import com.app.booking.dto.external.ApiResponseWrapper;
import lombok.extern.slf4j. Slf4j;
import org. springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    @Override
    public ApiResponseWrapper<List<Map<String, Object>>> getAvailableTechnicians() {
        log.warn("FALLBACK: Auth service unavailable - getAvailableTechnicians()");
        return new ApiResponseWrapper<>(false, "Auth service unavailable", Collections.emptyList());
    }

    @Override
    public ApiResponseWrapper<Map<String, Object>> getTechnicianByUserId(String technicianUserId) {
        log.warn("FALLBACK: Auth service unavailable - getTechnicianByUserId({})", technicianUserId);
        return createFallbackResponse("Auth service unavailable", null);
    }

    @Override
    public ApiResponseWrapper<Map<String, Object>> validateTechnician(String technicianUserId) {
        log.warn("FALLBACK: Auth service unavailable - validateTechnician({})", technicianUserId);
        Map<String, Object> fallbackData = new HashMap<>();
        fallbackData.put("exists", false);
        fallbackData.put("approved", false);
        fallbackData.put("available", false);
        fallbackData.put("canAssign", false);
        fallbackData.put("fallback", true);
        return new ApiResponseWrapper<>(false, "Auth service unavailable - fallback response", fallbackData);
    }

    @Override
    public ApiResponseWrapper<Void> incrementTechnicianJobs(String technicianUserId) {
        log.warn("FALLBACK: Auth service unavailable - incrementTechnicianJobs({})", technicianUserId);
        return new ApiResponseWrapper<>(false, "Auth service unavailable - will retry later", null);
    }

    @Override
    public ApiResponseWrapper<Void> updateTechnicianAvailability(String technicianUserId, boolean available) {
        log.warn("FALLBACK: Auth service unavailable - updateTechnicianAvailability({}, {})",
                technicianUserId, available);
        return new ApiResponseWrapper<>(false, "Auth service unavailable - will retry later", null);
    }

    @Override
    public ApiResponseWrapper<List<Map<String, Object>>> getTechniciansBySkill(String skill) {
        log.warn("FALLBACK: Auth service unavailable - getTechniciansBySkill({})", skill);
        return new ApiResponseWrapper<>(false, "Auth service unavailable", Collections.emptyList());
    }

    private <T> ApiResponseWrapper<T> createFallbackResponse(String message, T data) {
        return new ApiResponseWrapper<>(false, message + " - fallback response", data);
    }
}