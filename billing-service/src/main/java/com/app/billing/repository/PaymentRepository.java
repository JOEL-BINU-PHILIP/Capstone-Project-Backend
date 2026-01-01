package com.app.billing.repository;

import com.app.billing. model.Payment;
import com.app.billing.model.PaymentMethod;
import com.app.billing.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java. util.List;
import java. util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {

    // Find by payment number
    Optional<Payment> findByPaymentNumber(String paymentNumber);

    // Find by invoice
    List<Payment> findByInvoiceIdOrderByCreatedAtDesc(String invoiceId);

    // Find by customer
    List<Payment> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    Page<Payment> findByCustomerId(String customerId, Pageable pageable);

    // By status
    List<Payment> findByStatus(PaymentStatus status);

    // By payment method
    List<Payment> findByPaymentMethod(PaymentMethod paymentMethod);

    // Date range
    @Query("{'createdAt': {$gte: ?0, $lte: ?1}}")
    List<Payment> findByCreatedAtBetween(Instant start, Instant end);

    // Sum payments for invoice
    @Aggregation(pipeline = {
            "{ $match: { invoiceId: ?0, status: 'COMPLETED' } }",
            "{ $group: { _id: null, total: { $sum: '$amount' } } }"
    })
    Double sumCompletedPaymentsByInvoiceId(String invoiceId);

    // Statistics
    long countByStatus(PaymentStatus status);

    @Aggregation(pipeline = {
            "{ $match: { status: 'COMPLETED', createdAt: { $gte: ?0, $lte: ?1 } } }",
            "{ $group: { _id: null, total: { $sum: '$amount' } } }"
    })
    Double sumCompletedPaymentsBetween(Instant start, Instant end);
}