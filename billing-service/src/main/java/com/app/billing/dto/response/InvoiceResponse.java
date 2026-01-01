package com.app.billing. dto.response;

import com. app.billing.model.Invoice;
import com.app.billing.model.InvoiceStatus;
import lombok. AllArgsConstructor;
import lombok.Builder;
import lombok. Data;
import lombok.NoArgsConstructor;

import java. time. Instant;
import java.time.LocalDate;
import java. util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private String id;
    private String invoiceNumber;

    // Booking Reference
    private String bookingId;
    private String bookingNumber;

    // Customer Info
    private String customerId;
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
    private List<Invoice.LineItem> lineItems;

    // Pricing
    private Double subtotal;
    private Double taxPercentage;
    private Double taxAmount;
    private Double discountPercentage;
    private Double discountAmount;
    private Double totalAmount;
    private String currency;

    // Payment Tracking
    private Double amountPaid;
    private Double balanceDue;

    // Status
    private InvoiceStatus status;

    // Dates
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private Instant paidAt;

    // Notes
    private String notes;

    // Audit
    private Instant createdAt;
    private Instant updatedAt;
}