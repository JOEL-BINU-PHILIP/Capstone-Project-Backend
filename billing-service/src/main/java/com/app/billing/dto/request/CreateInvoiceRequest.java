package com.app.billing. dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceRequest {

    // Booking Reference
    @NotBlank(message = "Booking ID is required")
    private String bookingId;

    private String bookingNumber;

    // Customer Info
    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    private String customerEmail;
    private String customerPhone;

    // Service Info
    private String serviceId;
    private String serviceName;
    private String categoryName;

    // Technician Info
    private String technicianId;
    private String technicianName;

    // Pricing
    @NotNull(message = "Base price is required")
    @Positive(message = "Base price must be positive")
    private Double basePrice;

    private Double taxPercentage;       // Default 18% if not provided
    private Double discountPercentage;  // Default 0% if not provided

    // Notes
    private String notes;
}