package com.app.billing. dto.response;

import com. app.billing.model.PaymentMethod;
import com.app.billing.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String id;
    private String paymentNumber;

    // References
    private String invoiceId;
    private String invoiceNumber;
    private String bookingId;
    private String customerId;
    private String customerName;

    // Payment Details
    private Double amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;

    // Transaction Info
    private String transactionId;

    // Notes
    private String notes;

    // Refund Info
    private Boolean isRefund;
    private String refundReason;

    // Audit
    private String processedBy;
    private Instant createdAt;
    private Instant processedAt;
}