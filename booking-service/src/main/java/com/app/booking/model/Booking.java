package com.app.booking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data. annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb. core.index.Indexed;
import org. springframework.data.mongodb.core. mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java. util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bookings")
public class Booking {

    @Id
    private String id;

    @Indexed(unique = true)
    private String bookingNumber;           // BK-2026-00001

    // References
    @Indexed
    private String customerId;              // User ID of customer
    private String customerName;            // Snapshot of customer name
    private String customerPhone;           // Snapshot of customer phone
    private String customerEmail;           // Snapshot of customer email

    @Indexed
    private String serviceId;               // Reference to service catalog
    private String serviceName;             // Snapshot of service name
    private String categoryId;              // Reference to category
    private String categoryName;            // Snapshot of category name

    @Indexed
    private String technicianId;            // Assigned technician user ID
    private String technicianName;          // Snapshot of technician name
    private String technicianPhone;         // Snapshot of technician phone

    // Status & Priority
    @Indexed
    private BookingStatus status;
    private Priority priority;

    // Problem Description
    private String problemDescription;
//    private List<String> imageUrls;         // Images of the issue

    // Scheduling
    private LocalDateTime scheduledDate;
    private LocalDateTime scheduledTime;
    private Integer estimatedDurationMinutes;

    // Address
    private AddressDetails serviceAddress;

    // Pricing
    private PricingDetails pricing;

    // Assignment
    private String assignedBy;              // Manager who assigned
    private Instant assignedAt;

    // OTP Verification
    private String completionOtp;           // OTP to verify service completion
    private Boolean otpVerified;

    // Special Instructions
    private String specialInstructions;

    // Cancellation
    private CancellationDetails cancellation;

    // Rating & Feedback
    private RatingFeedback ratingFeedback;

    // Technician Notes
    private String technicianNotes;
//    private List<String> completionImageUrls;   // Before/after images

    // Lifecycle Timestamps
    private Instant confirmedAt;
    private Instant startedAt;
    private Instant completedAt;

    // Audit
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
