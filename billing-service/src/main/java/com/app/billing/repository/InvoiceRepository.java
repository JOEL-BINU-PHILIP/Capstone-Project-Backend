package com.app.billing.repository;

import com.app.billing.model.Invoice;
import com.app.billing.model.InvoiceStatus;
import org. springframework.data.domain.Page;
import org.springframework.data. domain.Pageable;
import org.springframework.data.mongodb.repository. Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time. Instant;
import java.time.LocalDate;
import java. util.List;
import java. util.Optional;

public interface InvoiceRepository extends MongoRepository<Invoice, String> {

    // Find by invoice number
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    // Check if invoice exists for booking
    boolean existsByBookingId(String bookingId);

    // Find by booking
    Optional<Invoice> findByBookingId(String bookingId);

    // Customer invoices
    List<Invoice> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    Page<Invoice> findByCustomerId(String customerId, Pageable pageable);

    // By status
    List<Invoice> findByStatus(InvoiceStatus status);
    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    // Overdue invoices
    @Query("{'status': 'PENDING', 'dueDate': {$lt: ?0}}")
    List<Invoice> findOverdueInvoices(LocalDate today);

    // Date range queries
    @Query("{'createdAt': {$gte:  ?0, $lte: ? 1}}")
    List<Invoice> findByCreatedAtBetween(Instant start, Instant end);

    @Query("{'invoiceDate': {$gte: ?0, $lte: ?1}}")
    List<Invoice> findByInvoiceDateBetween(LocalDate start, LocalDate end);

    // Statistics
    long countByStatus(InvoiceStatus status);
    long countByCustomerId(String customerId);

    // Sum total amounts
    @Aggregation(pipeline = {
            "{ $match: { status: ?0 } }",
            "{ $group:  { _id: null, total: { $sum: '$totalAmount' } } }"
    })
    Double sumTotalAmountByStatus(String status);

    // Search
    @Query("{'$or': [{'invoiceNumber': {$regex: ? 0, $options: 'i'}}, {'customerName': {$regex: ?0, $options: 'i'}}, {'bookingNumber': {$regex: ? 0, $options: 'i'}}]}")
    List<Invoice> searchInvoices(String query);
}