package com.app. booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok. NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatsResponse {
    private long totalBookings;
    private long pendingBookings;
    private long assignedBookings;
    private long inProgressBookings;
    private long completedBookings;
    private long cancelledBookings;
    private Map<String, Long> bookingsByCategory;
    private Map<String, Long> bookingsByStatus;
}