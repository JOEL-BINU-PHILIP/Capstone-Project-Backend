package com.app.billing.client;

import com.app.billing.client.fallback.AuthServiceFallback;
import com.app. billing.config.FeignConfig;
import com.app.billing.dto.external.ApiResponseWrapper;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework. web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(
        name = "auth-service",
        url = "${services.auth-service.url:http://localhost:8081}",
        configuration = FeignConfig.class,
        fallback = AuthServiceFallback. class
)
public interface AuthServiceClient {

    /**
     * Get user details by ID
     */
    @GetMapping("/api/internal/users/{userId}")
    ApiResponseWrapper<Map<String, Object>> getUserById(@PathVariable("userId") String userId);

    /**
     * Get user details by username
     */
    @GetMapping("/api/internal/users/username/{username}")
    ApiResponseWrapper<Map<String, Object>> getUserByUsername(@PathVariable("username") String username);

    /**
     * Validate if user exists
     */
    @GetMapping("/api/internal/users/{userId}/validate")
    ApiResponseWrapper<Map<String, Boolean>> validateUser(@PathVariable("userId") String userId);
}