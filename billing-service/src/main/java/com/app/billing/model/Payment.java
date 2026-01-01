package com.app.billing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org. springframework.data.mongodb.core. mapping.Document;

import java. time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    @Indexed(unique = true)
    private String paymentNumber;           // PAY-2026-00001

    // References
    @Indexed
    private String invoiceId;
    private String invoiceNumber;

    @Indexed
    private String bookingId;

    @Indexed
    private String customerId;
    private String customerName;

    // Payment Details
    private Double amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;

    // Transaction Info
    private String transactionId;           // External payment gateway transaction ID
    private String gatewayResponse;         // Response from payment gateway

    // Notes
    private String notes;

    // Refund Info (if applicable)
    private Boolean isRefund;
    private String refundReason;
    private String originalPaymentId;       // For refund payments

    // Audit
    private String processedBy;             // User who recorded the payment

    @CreatedDate
    private Instant createdAt;

    private Instant processedAt;
}