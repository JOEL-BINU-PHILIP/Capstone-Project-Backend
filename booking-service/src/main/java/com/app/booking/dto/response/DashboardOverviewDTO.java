package com.app.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDTO {

    // Booking counts
    private long totalBookings;
    private long pendingBookings;
    private long assignedBookings;
    private long inProgressBookings;
    private long completedBookings;
    private long cancelledBookings;

    // Today's stats
    private long todayBookings;
    private long todayCompleted;

    // This week's stats
    private long weekBookings;
    private long weekCompleted;

    // This month's stats
    private long monthBookings;
    private long monthCompleted;

    // Averages
    private double avgRating;
    private double avgResolutionTimeHours;

    // Breakdown
    private Map<String, Long> bookingsByStatus;
    private Map<String, Long> bookingsByCategory;
}