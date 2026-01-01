package com.app.booking. dto.response;

import lombok. AllArgsConstructor;
import lombok.Builder;
import lombok. Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianWorkloadDTO {

    private String technicianId;
    private String technicianName;

    // Current workload
    private long assignedBookings;
    private long inProgressBookings;
    private long totalActiveBookings;

    // Completed
    private long completedBookings;
    private long completedThisMonth;

    // Status
    private String workloadStatus; // LOW, MEDIUM, HIGH, OVERLOADED
}