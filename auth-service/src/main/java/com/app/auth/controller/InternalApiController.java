package com.app.auth.controller;

import com.app.auth.model.TechnicianProfile;
import com.app.auth.model.User;
import com.app.auth.payload.ApiResponse;
import com.app. auth.repository.TechnicianProfileRepository;
import com.app.auth. repository.UserRepository;
import com.app.auth.service.TechnicianProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework. web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream. Collectors;

/**
 * Internal APIs for inter-service communication.
 * These endpoints are called by other microservices (Booking, Billing).
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalApiController {

    private final UserRepository userRepository;
    private final TechnicianProfileRepository technicianProfileRepository;
    private final TechnicianProfileService technicianProfileService;

    // ==================== USER APIs ====================

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserById(
            @PathVariable String userId
    ) {
        log.debug("Internal API: Getting user by ID: {}", userId);

        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(
                    new ApiResponse<>(false, "User not found", null)
            );
        }

        User user = userOpt.get();
        Map<String, Object> userData = buildUserData(user);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "User found", userData)
        );
    }

    @GetMapping("/users/username/{username}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserByUsername(
            @PathVariable String username
    ) {
        log.debug("Internal API: Getting user by username: {}", username);

        Optional<User> userOpt = userRepository. findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(
                    new ApiResponse<>(false, "User not found", null)
            );
        }

        User user = userOpt.get();
        Map<String, Object> userData = buildUserData(user);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "User found", userData)
        );
    }

    @GetMapping("/users/{userId}/validate")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> validateUser(
            @PathVariable String userId
    ) {
        log.debug("Internal API:  Validating user: {}", userId);

        boolean exists = userRepository.existsById(userId);

        Map<String, Boolean> result = new HashMap<>();
        result.put("exists", exists);
        result.put("valid", exists);

        return ResponseEntity.ok(
                new ApiResponse<>(true, exists ? "User exists" : "User not found", result)
        );
    }

    // ==================== TECHNICIAN APIs ====================

    @GetMapping("/technicians/available")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableTechnicians() {
        log.debug("Internal API: Getting available technicians");

        List<TechnicianProfile> technicians = technicianProfileRepository.findAvailableTechnicians();

        List<Map<String, Object>> technicianList = technicians.stream()
                .map(this::buildTechnicianData)
                .collect(Collectors. toList());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Available technicians retrieved", technicianList)
        );
    }

    @GetMapping("/technicians/{technicianUserId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTechnicianByUserId(
            @PathVariable String technicianUserId
    ) {
        log.debug("Internal API: Getting technician by user ID: {}", technicianUserId);

        Optional<TechnicianProfile> profileOpt = technicianProfileRepository.findByUserId(technicianUserId);

        if (profileOpt. isEmpty()) {
            return ResponseEntity.ok(
                    new ApiResponse<>(false, "Technician not found", null)
            );
        }

        Map<String, Object> technicianData = buildTechnicianData(profileOpt.get());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Technician found", technicianData)
        );
    }

    @GetMapping("/technicians/{technicianUserId}/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateTechnician(
            @PathVariable String technicianUserId
    ) {
        log.debug("Internal API: Validating technician: {}", technicianUserId);

        Optional<TechnicianProfile> profileOpt = technicianProfileRepository. findByUserId(technicianUserId);

        Map<String, Object> result = new HashMap<>();

        if (profileOpt.isEmpty()) {
            result.put("exists", false);
            result. put("approved", false);
            result.put("available", false);
            result.put("canAssign", false);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Technician not found", result)
            );
        }

        TechnicianProfile profile = profileOpt.get();
        boolean isApproved = profile.getApprovalStatus() == TechnicianProfile.ApprovalStatus.APPROVED;
        boolean isAvailable = profile. isAvailable();

        result.put("exists", true);
        result.put("approved", isApproved);
        result.put("available", isAvailable);
        result.put("canAssign", isApproved && isAvailable);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Technician validation complete", result)
        );
    }

    @PutMapping("/technicians/{technicianUserId}/increment-jobs")
    public ResponseEntity<ApiResponse<Void>> incrementTechnicianJobs(
            @PathVariable String technicianUserId
    ) {
        log.debug("Internal API: Incrementing jobs for technician: {}", technicianUserId);

        Optional<TechnicianProfile> profileOpt = technicianProfileRepository. findByUserId(technicianUserId);

        if (profileOpt.isEmpty()) {
            return ResponseEntity.ok(
                    new ApiResponse<>(false, "Technician not found", null)
            );
        }

        TechnicianProfile profile = profileOpt.get();
        profile.setTotalJobsCompleted(profile.getTotalJobsCompleted() + 1);
        technicianProfileRepository.save(profile);

        log.info("Technician {} jobs incremented to {}", technicianUserId, profile.getTotalJobsCompleted());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Jobs count incremented", null)
        );
    }

    @PutMapping("/technicians/{technicianUserId}/availability")
    public ResponseEntity<ApiResponse<Void>> updateTechnicianAvailability(
            @PathVariable String technicianUserId,
            @RequestParam boolean available
    ) {
        log.debug("Internal API:  Updating availability for technician: {} to {}", technicianUserId, available);

        Optional<TechnicianProfile> profileOpt = technicianProfileRepository.findByUserId(technicianUserId);

        if (profileOpt.isEmpty()) {
            return ResponseEntity.ok(
                    new ApiResponse<>(false, "Technician not found", null)
            );
        }

        TechnicianProfile profile = profileOpt.get();
        profile.setAvailable(available);
        technicianProfileRepository.save(profile);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Availability updated", null)
        );
    }

    @GetMapping("/technicians/by-skill")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTechniciansBySkill(
            @RequestParam String skill
    ) {
        log.debug("Internal API: Getting technicians with skill: {}", skill);

        List<TechnicianProfile> technicians = technicianProfileRepository
                .findByCityAndSkills(null, List.of(skill));

        List<Map<String, Object>> technicianList = technicians.stream()
                .map(this:: buildTechnicianData)
                .collect(Collectors.toList());

        return ResponseEntity. ok(
                new ApiResponse<>(true, "Technicians retrieved", technicianList)
        );
    }

    // ==================== HELPER METHODS ====================

    private Map<String, Object> buildUserData(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("username", user.getUsername());
        userData.put("email", user.getEmail());
        userData.put("firstName", user.getFirstName());
        userData.put("lastName", user.getLastName());

        // FIX: Proper null handling for fullName
        String fullName = buildFullName(user. getFirstName(), user.getLastName());
        userData.put("fullName", fullName);

        userData.put("phoneNumber", user.getPhoneNumber());
        userData.put("city", user.getCity());
        userData.put("state", user. getState());
        userData.put("zipCode", user.getZipCode());
        userData.put("roles", user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toList()));
        userData.put("enabled", user.isEnabled());
        userData.put("emailVerified", user.isEmailVerified());
        return userData;
    }

    private Map<String, Object> buildTechnicianData(TechnicianProfile profile) {
        Map<String, Object> data = new HashMap<>();

        // Get user details
        Optional<User> userOpt = userRepository.findById(profile.getUserId());

        data.put("id", profile.getId());
        data.put("userId", profile.getUserId());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            data.put("username", user.getUsername());
            data.put("email", user.getEmail());
            data.put("firstName", user.getFirstName());
            data.put("lastName", user.getLastName());

            // FIX: Proper null handling for fullName
            String fullName = buildFullName(user.getFirstName(), user.getLastName());
            data.put("fullName", fullName);

            data.put("phoneNumber", user.getPhoneNumber());
        }

        data.put("skills", profile.getSkills());
        data.put("experienceYears", profile.getExperienceYears());
        data.put("bio", profile.getBio());
        data.put("city", profile. getCity());
        data.put("state", profile.getState());
        data.put("approvalStatus", profile.getApprovalStatus().name());
        data.put("available", profile.isAvailable());
        data.put("averageRating", profile.getAverageRating());
        data.put("totalJobsCompleted", profile.getTotalJobsCompleted());
        data.put("currentActiveJobs", profile.getCurrentActiveJobs());

        return data;
    }

    /**
     * Build full name from first and last name with proper null handling
     */
    private String buildFullName(String firstName, String lastName) {
        StringBuilder sb = new StringBuilder();

        if (firstName != null && ! firstName.isEmpty()) {
            sb.append(firstName);
        }

        if (lastName != null && ! lastName.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(lastName);
        }

        return sb.toString().isEmpty() ? "Unknown" : sb.toString();
    }
}