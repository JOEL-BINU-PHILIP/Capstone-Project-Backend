package com.app.billing.model;

import lombok. AllArgsConstructor;
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
import java.util.ArrayList;
import java.util.List;

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
    @Indexed
    private String bookingId;
    private String bookingNumber;

    // Customer Info (Snapshot)
    @Indexed
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerAddress;

    // Service Info (Snapshot)
    private String serviceId;
    private String serviceName;
    private String categoryName;

    // Technician Info (Snapshot)
    private String technicianId;
    private String technicianName;

    // Line Items
    @Builder.Default
    private List<LineItem> lineItems = new ArrayList<>();

    // Pricing
    private Double subtotal;
    private Double taxPercentage;
    private Double taxAmount;
    private Double discountPercentage;
    private Double discountAmount;
    private Double totalAmount;
    private String currency;

    // Payment Tracking
    @Builder.Default
    private Double amountPaid = 0.0;

    @Builder.Default
    private Double balanceDue = 0.0;

    // Status
    @Indexed
    private InvoiceStatus status;

    // Dates
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private Instant paidAt;

    // Notes
    private String notes;
    private String termsAndConditions;

    // Audit
    private String createdBy;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // Embedded Line Item
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItem {
        private String description;
        private Integer quantity;
        private Double unitPrice;
        private Double amount;
    }
}