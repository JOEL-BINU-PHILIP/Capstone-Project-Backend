package com.app.booking.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianDTO {
    private String id;
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phoneNumber;
    private Set<String> skills;
    private Integer experienceYears;
    private String bio;
    private String city;
    private String state;
    private String approvalStatus;
    private boolean available;
    private Double averageRating;
    private Integer totalJobsCompleted;
    private Integer currentActiveJobs;
}