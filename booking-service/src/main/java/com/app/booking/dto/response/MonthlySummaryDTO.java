package com. app.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryDTO {

    // Period
    private int year;
    private int month;
    private String monthName;

    // Booking stats
    private long totalBookings;
    private long completedBookings;
    private long cancelledBookings;
    private double completionRate;

    // Comparison with previous month
    private long previousMonthBookings;
    private double bookingsGrowthRate;

    // By category
    private Map<String, Long> bookingsByCategory;

    // By status
    private Map<String, Long> bookingsByStatus;

    // Service quality
    private double avgRating;
    private double avgResolutionTimeHours;

    // Top performing technician
    private String topTechnicianId;
    private String topTechnicianName;
    private long topTechnicianCompletions;
}