package com.app.billing.controller;

import com.app.billing.dto.request.CreateInvoiceRequest;
import com.app.billing.dto.request.PayInvoiceRequest;
import com.app.billing.dto.response.ApiResponse;
import com.app. billing.dto.response.InvoiceResponse;
import com.app.billing.dto.response.RevenueReportResponse;
import com.app.billing.model.InvoiceStatus;
import com.app.billing.service. InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org. springframework.data.domain.Page;
import org.springframework.data. domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework. http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util. List;

@Slf4j
@RestController
@RequestMapping("/api/billing/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    // ==================== CREATE ====================

    /**
     * Create a new invoice (when booking is completed)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            Authentication authentication
    ) {
        String createdBy = authentication.getName();
        InvoiceResponse response = invoiceService.createInvoice(request, createdBy);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse. success("Invoice created successfully", response));
    }

    // ==================== READ ====================

    /**
     * Get invoice by ID
     */
    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(
            @PathVariable String invoiceId
    ) {
        InvoiceResponse response = invoiceService.getInvoiceById(invoiceId);
        return ResponseEntity.ok(ApiResponse. success(response));
    }

    /**
     * Get invoice by invoice number
     */
    @GetMapping("/number/{invoiceNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByNumber(
            @PathVariable String invoiceNumber
    ) {
        InvoiceResponse response = invoiceService. getInvoiceByNumber(invoiceNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get invoice by booking ID
     */
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByBookingId(
            @PathVariable String bookingId
    ) {
        InvoiceResponse response = invoiceService.getInvoiceByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== LIST ====================

    /**
     * Get all invoices (paginated) - Manager/Admin only
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getAllInvoices(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<InvoiceResponse> response = invoiceService.getAllInvoicesPaged(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get invoices for logged-in customer
     */
    @GetMapping("/my-invoices")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getMyInvoices(
            Authentication authentication
    ) {
        String customerId = authentication.getName();
        List<InvoiceResponse> response = invoiceService.getCustomerInvoices(customerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get invoices by customer ID - Manager/Admin only
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getCustomerInvoices(
            @PathVariable String customerId
    ) {
        List<InvoiceResponse> response = invoiceService.getCustomerInvoices(customerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get invoices by status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesByStatus(
            @PathVariable InvoiceStatus status
    ) {
        List<InvoiceResponse> response = invoiceService.getInvoicesByStatus(status);
        return ResponseEntity. ok(ApiResponse.success(response));
    }

    // ==================== PAY ====================

    /**
     * Pay an invoice (simple button click)
     * Can be called by Customer (for their own invoice) or Manager/Admin
     */
    @PostMapping("/{invoiceId}/pay")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> payInvoice(
            @PathVariable String invoiceId,
            @Valid @RequestBody PayInvoiceRequest request,
            Authentication authentication
    ) {
        String paidBy = authentication.getName();

        // For customers, verify they own this invoice
        // This check can be enhanced based on your requirements

        InvoiceResponse response = invoiceService.payInvoice(invoiceId, request, paidBy);
        return ResponseEntity. ok(ApiResponse.success("Payment successful!", response));
    }

    // ==================== CANCEL ====================

    /**
     * Cancel an invoice
     */
    @PostMapping("/{invoiceId}/cancel")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> cancelInvoice(
            @PathVariable String invoiceId,
            @RequestParam String reason
    ) {
        InvoiceResponse response = invoiceService.cancelInvoice(invoiceId, reason);
        return ResponseEntity.ok(ApiResponse.success("Invoice cancelled", response));
    }

    // ==================== REPORTS ====================

    /**
     * Get revenue report for a date range
     */
    @GetMapping("/reports/revenue")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        RevenueReportResponse response = invoiceService.getRevenueReport(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== SEARCH ====================

    /**
     * Search invoices
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> searchInvoices(
            @RequestParam String query
    ) {
        List<InvoiceResponse> response = invoiceService.searchInvoices(query);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}