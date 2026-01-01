package com.app.billing.service;

import com.app.billing.dto.request.RecordPaymentRequest;
import com. app.billing.dto.response. PaymentResponse;
import com. app.billing.model.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain. Pageable;

import java.time.LocalDate;
import java.util. List;
import java.util.Map;

public interface PaymentService {

    // Record payment
    PaymentResponse recordPayment(RecordPaymentRequest request, String processedBy);

    // Read
    PaymentResponse getPaymentById(String paymentId);
    PaymentResponse getPaymentByNumber(String paymentNumber);

    // List
    List<PaymentResponse> getPaymentsByInvoice(String invoiceId);
    List<PaymentResponse> getPaymentsByCustomer(String customerId);
    Page<PaymentResponse> getPaymentsByCustomerPaged(String customerId, Pageable pageable);
    Page<PaymentResponse> getAllPaymentsPaged(Pageable pageable);

    // Refund
    PaymentResponse processRefund(String paymentId, String reason, String processedBy);

    // Reports
    Map<String, Double> getPaymentMethodBreakdown(LocalDate startDate, LocalDate endDate);
    Double getTotalPaymentsForPeriod(LocalDate startDate, LocalDate endDate);
}