package com.app.billing.service;

import com.app.billing.dto.request.CreateInvoiceRequest;
import com.app.billing.dto. request.PayInvoiceRequest;
import com.app.billing.dto. response.InvoiceResponse;
import com.app.billing. dto.response.RevenueReportResponse;
import com.app.billing.model.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain. Pageable;

import java.time.LocalDate;
import java. util.List;

public interface InvoiceService {

    // Create
    InvoiceResponse createInvoice(CreateInvoiceRequest request, String createdBy);

    // Read
    InvoiceResponse getInvoiceById(String invoiceId);
    InvoiceResponse getInvoiceByNumber(String invoiceNumber);
    InvoiceResponse getInvoiceByBookingId(String bookingId);

    // ========== NEW:  Unified List Method ==========
    /**
     * Get invoices with optional filters
     * @param customerId - Filter by customer ID (null for all)
     * @param status - Filter by status (null for all)
     * @param currentUser - Current authenticated user (for "me" filter)
     * @param isManager - Whether the current user is a manager/admin
     * @param pageable - Pagination parameters
     */
    Page<InvoiceResponse> getInvoices(
            String customerId,
            InvoiceStatus status,
            String currentUser,
            boolean isManager,
            Pageable pageable
    );

    // ========== KEEP: Legacy methods for backward compatibility (can be deprecated) ==========
    List<InvoiceResponse> getCustomerInvoices(String customerId);
    Page<InvoiceResponse> getCustomerInvoicesPaged(String customerId, Pageable pageable);
    List<InvoiceResponse> getInvoicesByStatus(InvoiceStatus status);
    Page<InvoiceResponse> getAllInvoicesPaged(Pageable pageable);

    // Pay - Simple button click
    InvoiceResponse payInvoice(String invoiceId, PayInvoiceRequest request, String paidBy);

    // Cancel
    InvoiceResponse cancelInvoice(String invoiceId, String reason);

    // Reports
    RevenueReportResponse getRevenueReport(LocalDate startDate, LocalDate endDate);

    // Search
    List<InvoiceResponse> searchInvoices(String query);
}