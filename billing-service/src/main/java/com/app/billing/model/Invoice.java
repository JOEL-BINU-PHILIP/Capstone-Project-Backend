package com.app.billing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb. core.index.Indexed;
import org. springframework.data.mongodb.core. mapping.Document;

import java.time. Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "invoices")
public class Invoice {

    @Id
    private String id;

    @Indexed(unique = true)
    private String invoiceNumber;           // INV-2026-00001

    // Booking Reference
    @Indexed(unique = true)
    private String bookingId;
    private String bookingNumber;

    // Customer Info (Snapshot)
    @Indexed
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Service Info (Snapshot)
    private String serviceId;
    private String serviceName;
    private String categoryName;

    // Technician Info (Snapshot)
    private String technicianId;
    private String technicianName;

    // Pricing
    private Double basePrice;
    private Double taxPercentage;
    private Double taxAmount;
    private Double discountPercentage;
    private Double discountAmount;
    private Double totalAmount;

    @Builder.Default
    private String currency = "INR";

    // Status
    @Indexed
    private InvoiceStatus status;

    // Payment Info (filled when paid)
    private PaymentMethod paymentMethod;
    private Instant paidAt;
    private String paidBy;              // User who clicked pay button

    // Dates
    private LocalDate invoiceDate;
    private LocalDate dueDate;

    // Notes
    private String notes;

    // Audit
    private String createdBy;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}