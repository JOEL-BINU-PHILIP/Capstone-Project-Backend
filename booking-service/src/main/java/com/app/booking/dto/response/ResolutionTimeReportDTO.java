package com.app.booking. dto.response;

import lombok. AllArgsConstructor;
import lombok.Builder;
import lombok. Data;
import lombok.NoArgsConstructor;

import java. util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionTimeReportDTO {

    // Overall averages
    private double avgResolutionTimeHours;
    private double avgAssignmentTimeHours;  // Time from created to assigned
    private double avgServiceTimeHours;     // Time from started to completed

    // Min/Max
    private double minResolutionTimeHours;
    private double maxResolutionTimeHours;

    // By category
    private Map<String, Double> avgTimeByCategory;

    // By priority
    private Map<String, Double> avgTimeByPriority;

    // Sample size
    private long totalCompletedBookings;
}