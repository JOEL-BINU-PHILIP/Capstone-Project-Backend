package com.app.billing.service.impl;

import com.app.billing.dto.request.RecordPaymentRequest;
import com.app.billing.dto.response.PaymentResponse;
import com.app.billing.exception.InvalidPaymentException;
import com.app.billing.exception. ResourceNotFoundException;
import com.app.billing.model.Invoice;
import com.app.billing.model.Payment;
import com.app.billing.model.PaymentMethod;
import com.app.billing.model.PaymentStatus;
import com.app.billing.repository.InvoiceRepository;
import com.app.billing.repository. PaymentRepository;
import com. app.billing.service.InvoiceService;
import com.app.billing.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok. extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util. List;
import java.util. Map;
import java.util. Random;
import java.util. stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;

    @Override
    @Transactional
    public PaymentResponse recordPayment(RecordPaymentRequest request, String processedBy) {

        // Get invoice
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + request. getInvoiceId()));

        // Validate payment
        if (invoice.getBalanceDue() <= 0) {
            throw new InvalidPaymentException("Invoice is already fully paid");
        }

        if (request.getAmount() > invoice.getBalanceDue()) {
            throw new InvalidPaymentException("Payment amount exceeds balance due.  Balance due: " + invoice.getBalanceDue());
        }

        // Create payment record
        Payment payment = Payment.builder()
                .paymentNumber(generatePaymentNumber())
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .bookingId(invoice.getBookingId())
                .customerId(invoice.getCustomerId())
                .customerName(invoice.getCustomerName())
                .amount(request.getAmount())
                .currency(invoice.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus. COMPLETED)
                .transactionId(request.getTransactionId())
                .notes(request.getNotes())
                .isRefund(false)
                .processedBy(processedBy)
                .processedAt(Instant. now())
                .build();

        Payment saved = paymentRepository. save(payment);

        // Update invoice payment status
        invoiceService.updateInvoicePayment(invoice.getId(), request.getAmount());

        log.info("Payment recorded:  {} for invoice: {}, amount:  {}",
                saved.getPaymentNumber(), invoice.getInvoiceNumber(), request.getAmount());

        return toPaymentResponse(saved);
    }

    @Override
    public PaymentResponse getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        return toPaymentResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentByNumber(String paymentNumber) {
        Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentNumber));
        return toPaymentResponse(payment);
    }

    @Override
    public List<PaymentResponse> getPaymentsByInvoice(String invoiceId) {
        return paymentRepository.findByInvoiceIdOrderByCreatedAtDesc(invoiceId)
                .stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentResponse> getPaymentsByCustomer(String customerId) {
        return paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PaymentResponse> getPaymentsByCustomerPaged(String customerId, Pageable pageable) {
        return paymentRepository.findByCustomerId(customerId, pageable)
                .map(this::toPaymentResponse);
    }

    @Override
    public Page<PaymentResponse> getAllPaymentsPaged(Pageable pageable) {
        return paymentRepository.findAll(pageable)
                .map(this::toPaymentResponse);
    }

    @Override
    @Transactional
    public PaymentResponse processRefund(String paymentId, String reason, String processedBy) {

        // Get original payment
        Payment originalPayment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found:  " + paymentId));

        // Validate refund
        if (originalPayment.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidPaymentException("Can only refund completed payments");
        }

        if (originalPayment.getIsRefund() != null && originalPayment.getIsRefund()) {
            throw new InvalidPaymentException("Cannot refund a refund transaction");
        }

        // Check if already refunded
        List<Payment> existingRefunds = paymentRepository. findByInvoiceIdOrderByCreatedAtDesc(originalPayment.getInvoiceId())
                .stream()
                .filter(p -> p.getIsRefund() != null && p.getIsRefund())
                .filter(p -> originalPayment.getId().equals(p.getOriginalPaymentId()))
                .collect(Collectors.toList());

        if (!existingRefunds.isEmpty()) {
            throw new InvalidPaymentException("This payment has already been refunded");
        }

        // Create refund payment record
        Payment refundPayment = Payment.builder()
                .paymentNumber(generatePaymentNumber())
                .invoiceId(originalPayment.getInvoiceId())
                .invoiceNumber(originalPayment.getInvoiceNumber())
                .bookingId(originalPayment.getBookingId())
                .customerId(originalPayment.getCustomerId())
                .customerName(originalPayment.getCustomerName())
                .amount(-originalPayment.getAmount())  // Negative amount for refund
                .currency(originalPayment. getCurrency())
                .paymentMethod(originalPayment.getPaymentMethod())
                .status(PaymentStatus.REFUNDED)
                .isRefund(true)
                .refundReason(reason)
                .originalPaymentId(originalPayment. getId())
                .processedBy(processedBy)
                .processedAt(Instant.now())
                .build();

        Payment savedRefund = paymentRepository.save(refundPayment);

        // Update original payment status
        originalPayment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(originalPayment);

        // Update invoice - subtract the refunded amount
        invoiceService.updateInvoicePayment(originalPayment.getInvoiceId(), -originalPayment.getAmount());

        log.info("Refund processed:  {} for original payment: {}, amount: {}",
                savedRefund.getPaymentNumber(), originalPayment.getPaymentNumber(), originalPayment.getAmount());

        return toPaymentResponse(savedRefund);
    }

    @Override
    public Map<String, Double> getPaymentMethodBreakdown(LocalDate startDate, LocalDate endDate) {
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Payment> payments = paymentRepository.findByCreatedAtBetween(startInstant, endInstant);

        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .filter(p -> p.getIsRefund() == null || ! p.getIsRefund())
                .collect(Collectors.groupingBy(
                        p -> p.getPaymentMethod().name(),
                        Collectors.summingDouble(Payment::getAmount)
                ));
    }

    @Override
    public Double getTotalPaymentsForPeriod(LocalDate startDate, LocalDate endDate) {
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Payment> payments = paymentRepository.findByCreatedAtBetween(startInstant, endInstant);

        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .filter(p -> p.getIsRefund() == null || !p.getIsRefund())
                .mapToDouble(Payment::getAmount)
                .sum();
    }

    // ==================== HELPER METHODS ====================

    private String generatePaymentNumber() {
        String year = String.valueOf(Year.now().getValue());
        String random = String.format("%05d", new Random().nextInt(100000));
        return "PAY-" + year + "-" + random;
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .invoiceId(payment.getInvoiceId())
                .invoiceNumber(payment.getInvoiceNumber())
                .bookingId(payment.getBookingId())
                .customerId(payment.getCustomerId())
                .customerName(payment.getCustomerName())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .notes(payment.getNotes())
                .isRefund(payment.getIsRefund())
                .refundReason(payment.getRefundReason())
                .processedBy(payment.getProcessedBy())
                .createdAt(payment.getCreatedAt())
                .processedAt(payment.getProcessedAt())
                .build();
    }
}