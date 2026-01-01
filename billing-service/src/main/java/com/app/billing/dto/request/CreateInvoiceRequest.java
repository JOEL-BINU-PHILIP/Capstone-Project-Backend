package com.app.billing.dto. request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java. util.List;

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
    private String customerAddress;

    // Service Info
    private String serviceId;
    private String serviceName;
    private String categoryName;

    // Technician Info
    private String technicianId;
    private String technicianName;

    // Line Items
    private List<LineItemRequest> lineItems;

    // Pricing
    @NotNull(message = "Subtotal is required")
    @Positive(message = "Subtotal must be positive")
    private Double subtotal;

    private Double taxPercentage;
    private Double discountPercentage;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private Double totalAmount;

    private String currency;

    // Dates
    private LocalDate dueDate;

    // Notes
    private String notes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItemRequest {
        private String description;
        private Integer quantity;
        private Double unitPrice;
    }
}