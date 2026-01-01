package com.app.billing.dto. external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    private String bookingId;
    private String bookingNumber;
    private String status;

    // Customer info
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Service info
    private String serviceId;
    private String serviceName;
    private String categoryName;

    // Technician info
    private String technicianId;
    private String technicianName;

    // Pricing
    private Double basePrice;
    private Double taxPercentage;
    private Double taxAmount;
    private Double discountPercentage;
    private Double discountAmount;
    private Double finalPrice;
    private String currency;

    // Address
    private String serviceAddress;

    // Dates
    private String scheduledDate;
    private String completedAt;
}