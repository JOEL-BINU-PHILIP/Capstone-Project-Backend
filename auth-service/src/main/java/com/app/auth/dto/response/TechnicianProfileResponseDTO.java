package com.app.auth.dto.response;

import com.app.auth.model.TechnicianProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianProfileResponseDTO {

    private String id;
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;

    private Set<String> skills;
    private Integer experienceYears;
    private String bio;

    private String city;
    private String state;

    private TechnicianProfile.ApprovalStatus approvalStatus;
    private String rejectionReason;

    private Double averageRating;
    private Integer totalJobsCompleted;
    private boolean available;

    private Instant createdAt;
}