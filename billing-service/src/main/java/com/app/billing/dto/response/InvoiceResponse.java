package com.app.billing.dto. response;

import com.app.billing.model.InvoiceStatus;
import com.app.billing.model.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private String id;
    private String invoiceNumber;

    // Booking
    private String bookingId;
    private String bookingNumber;

    // Customer
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Service
    private String serviceId;
    private String serviceName;
    private String categoryName;

    // Technician
    private String technicianId;
    private String technicianName;

    // Pricing
    private Double basePrice;
    private Double taxPercentage;
    private Double taxAmount;
    private Double discountPercentage;
    private Double discountAmount;
    private Double totalAmount;
    private String currency;

    // Status
    private InvoiceStatus status;
    private boolean isPaid;

    // Payment Info
    private PaymentMethod paymentMethod;
    private Instant paidAt;

    // Dates
    private LocalDate invoiceDate;
    private LocalDate dueDate;

    // Notes
    private String notes;

    // Audit
    private Instant createdAt;
    private Instant updatedAt;
}