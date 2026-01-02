package com.app.auth. dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok. Data;
import lombok.NoArgsConstructor;

import java.time. Instant;
import java.util. List;
import java.util. Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponseDTO {

    // Basic user info
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phoneNumber;

    // Address
    private String city;
    private String state;
    private String zipCode;

    // Account info
    private List<String> roles;
    private boolean enabled;
    private boolean emailVerified;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;

    // ========== TECHNICIAN-SPECIFIC FIELDS (null for non-technicians) ==========

    private TechnicianInfo technicianInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnicianInfo {
        private String technicianId;
        private Set<String> skills;
        private Integer experienceYears;
        private String bio;
        private String approvalStatus;      // PENDING, APPROVED, REJECTED
        private boolean available;
        private Double averageRating;
        private Integer totalJobsCompleted;
        private Integer currentActiveJobs;
        private Instant approvedAt;
        private String rejectionReason;
    }
}