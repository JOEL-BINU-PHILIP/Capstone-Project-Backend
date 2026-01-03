package com. app.billing.service;

import com. app.billing.dto.request.PayInvoiceRequest;
import com.app. billing.dto.response.InvoiceResponse;
import com.app. billing.dto.response.RevenueReportResponse;
import com.app.billing.model. InvoiceStatus;
import org.springframework.data. domain.Page;
import org.springframework. data.domain. Pageable;

import java.time. LocalDate;
import java.util.List;

public interface InvoiceService {

    // ============================================================
    // REMOVED: createInvoice(CreateInvoiceRequest request, String createdBy)
    // Manual invoice creation is no longer needed
    // ============================================================

    // ========== AUTO-GENERATION (Internal Use Only) ==========
    /**
     * Create invoice from booking ID.
     * Called automatically when booking is completed.
     * NOT exposed via REST API.
     */
    InvoiceResponse createInvoiceFromBooking(String bookingId, String createdBy);

    // ========== READ ==========
    InvoiceResponse getInvoiceById(String invoiceId);
    InvoiceResponse getInvoiceByNumber(String invoiceNumber);
    InvoiceResponse getInvoiceByBookingId(String bookingId);

    // ========== LIST ==========
    Page<InvoiceResponse> getInvoices(
            String customerId,
            InvoiceStatus status,
            String currentUser,
            boolean isManager,
            Pageable pageable
    );

    // ========== PAYMENT ==========
    InvoiceResponse payInvoice(String invoiceId, PayInvoiceRequest request, String paidBy);

    // ========== CANCEL ==========
    InvoiceResponse cancelInvoice(String invoiceId, String reason);

    // ========== REPORTS ==========
    RevenueReportResponse getRevenueReport(LocalDate startDate, LocalDate endDate);

    // ========== SEARCH ==========
    List<InvoiceResponse> searchInvoices(String query);
}