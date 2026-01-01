package com.app.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianPerformanceDTO {

    private String technicianId;
    private String technicianName;

    // Performance metrics
    private long totalJobsCompleted;
    private double avgRating;
    private int totalRatings;
    private double avgResolutionTimeHours;

    // Rejection rate
    private long totalAssigned;
    private long totalRejected;
    private double rejectionRate;

    // On-time completion
    private long onTimeCompletions;
    private long lateCompletions;
    private double onTimeRate;
}