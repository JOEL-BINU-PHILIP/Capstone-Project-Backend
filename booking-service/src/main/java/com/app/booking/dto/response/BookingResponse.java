package com.app.booking.dto. response;

import com.app. booking.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java. util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private String id;
    private String bookingNumber;

    // Customer Info
    private String customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    // Service Info
    private String serviceId;
    private String serviceName;
    private String categoryId;
    private String categoryName;

    // Technician Info
    private String technicianId;
    private String technicianName;
    private String technicianPhone;

    // Status
    private BookingStatus status;
    private Priority priority;

    // Details
    private String problemDescription;
    private List<String> imageUrls;
    private String specialInstructions;

    // Schedule
    private LocalDateTime scheduledDate;
    private Integer estimatedDurationMinutes;

    // Address
    private AddressDetails serviceAddress;

    // Pricing
    private PricingDetails pricing;

    // Rating
    private RatingFeedback ratingFeedback;

    // Timestamps
    private Instant assignedAt;
    private Instant confirmedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Cancellation (if cancelled)
    private CancellationDetails cancellation;
}