package com.app.booking.client;

import com.app.booking.client.fallback.AuthServiceFallback;
import com.app.booking.config.FeignConfig;
import com.app.booking.dto.external.ApiResponseWrapper;
import com.app.booking.dto.external.TechnicianDTO;
import com.app.booking.dto.external.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "auth-service",
        url = "${services.auth-service.url:http://localhost:8081}",
        configuration = FeignConfig.class,
        fallback = AuthServiceFallback.class
)
public interface AuthServiceClient {

    // ==================== USER APIs ====================

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

    // ==================== TECHNICIAN APIs ====================

    /**
     * Get all available technicians
     */
    @GetMapping("/api/internal/technicians/available")
    ApiResponseWrapper<List<Map<String, Object>>> getAvailableTechnicians();

    /**
     * Get technician details by user ID
     */
    @GetMapping("/api/internal/technicians/{technicianUserId}")
    ApiResponseWrapper<Map<String, Object>> getTechnicianByUserId(
            @PathVariable("technicianUserId") String technicianUserId);

    /**
     * Validate if technician exists and is available
     */
    @GetMapping("/api/internal/technicians/{technicianUserId}/validate")
    ApiResponseWrapper<Map<String, Object>> validateTechnician(
            @PathVariable("technicianUserId") String technicianUserId);

    /**
     * Increment technician's completed jobs count
     */
    @PutMapping("/api/internal/technicians/{technicianUserId}/increment-jobs")
    ApiResponseWrapper<Void> incrementTechnicianJobs(
            @PathVariable("technicianUserId") String technicianUserId);

    /**
     * Update technician availability
     */
    @PutMapping("/api/internal/technicians/{technicianUserId}/availability")
    ApiResponseWrapper<Void> updateTechnicianAvailability(
            @PathVariable("technicianUserId") String technicianUserId,
            @RequestParam("available") boolean available);

    /**
     * Get technicians by skill
     */
    @GetMapping("/api/internal/technicians/by-skill")
    ApiResponseWrapper<List<Map<String, Object>>> getTechniciansBySkill(
            @RequestParam("skill") String skill);
}