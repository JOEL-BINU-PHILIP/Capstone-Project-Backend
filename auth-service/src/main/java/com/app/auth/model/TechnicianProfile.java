package com.app.auth.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "technician_profiles")
public class TechnicianProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    // Professional details
    @Builder.Default
    private Set<String> skills = new HashSet<>();

    private Integer experienceYears;
    private String bio;

    // Location
    private String city;
    private String state;
    private Double latitude;
    private Double longitude;
    private Integer serviceRadiusKm;

    // Documents
    private String idProofUrl;
    private String idProofType; // AADHAAR, PAN, DRIVING_LICENSE
    private String certificateUrl;

    // Approval workflow
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    private String approvedByManagerId;
    private Instant approvedAt;
    private String rejectionReason;

    // Assignment
    private String assignedServiceId;
    private String assignedManagerId;

    // Performance metrics
    @Builder.Default
    private Double averageRating = 0.0;

    @Builder.Default
    private Integer totalJobsCompleted = 0;

    @Builder.Default
    private Integer totalJobsAssigned = 0;

    // Availability
    @Builder.Default
    private boolean available = true;

    @Builder.Default
    private Integer currentActiveJobs = 0;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;


    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        SUSPENDED
    }
}
