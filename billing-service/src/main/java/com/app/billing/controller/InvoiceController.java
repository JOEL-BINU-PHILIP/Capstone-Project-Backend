package com.app. billing.controller;

import com.app. billing.dto.request.CreateInvoiceRequest;
import com.app.billing. dto.request.PayInvoiceRequest;
import com.app.billing.dto. response.ApiResponse;
import com.app.billing. dto.response.InvoiceResponse;
import com.app. billing.dto.response.RevenueReportResponse;
import com.app.billing.model.InvoiceStatus;
import com.app.billing.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain. Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org. springframework.http.ResponseEntity;
import org.springframework.security. access.prepost. PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core. GrantedAuthority;
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
        String createdBy = authentication. getName();
        InvoiceResponse response = invoiceService.createInvoice(request, createdBy);

        return ResponseEntity. status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice created successfully", response));
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
        InvoiceResponse response = invoiceService. getInvoiceById(invoiceId);
        return ResponseEntity. ok(ApiResponse. success(response));
    }

    /**
     * Get invoice by invoice number
     */
    @GetMapping("/number/{invoiceNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByNumber(
            @PathVariable String invoiceNumber
    ) {
        InvoiceResponse response = invoiceService.getInvoiceByNumber(invoiceNumber);
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

    // ==================== LIST (CONSOLIDATED) ====================

    /**
     * Get invoices with optional filters
     *
     * Usage examples:
     * - GET /api/billing/invoices                           -> All invoices (Manager/Admin)
     * - GET /api/billing/invoices?user=me                   -> My invoices (Customer)
     * - GET /api/billing/invoices? customerId={id}           -> By customer (Manager/Admin)
     * - GET /api/billing/invoices?status=PENDING            -> By status (Manager/Admin)
     * - GET /api/billing/invoices?customerId={id}&status=PAID -> Combined filters
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getInvoices(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) InvoiceStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        String currentUser = authentication.getName();
        boolean isManager = isManagerOrAdmin(authentication);

        // Determine effective customer ID
        String effectiveCustomerId = null;

        if ("me".equalsIgnoreCase(user)) {
            // User wants their own invoices
            effectiveCustomerId = currentUser;
        } else if (customerId != null) {
            // Manager filtering by specific customer
            if (! isManager) {
                // Non-managers can only see their own invoices
                log.warn("Customer {} attempted to view invoices for customer {}", currentUser, customerId);
                effectiveCustomerId = currentUser;
            } else {
                effectiveCustomerId = customerId;
            }
        } else if (! isManager) {
            // Non-managers with no filter should only see their own invoices
            effectiveCustomerId = currentUser;
        }

        Page<InvoiceResponse> response = invoiceService. getInvoices(
                effectiveCustomerId,
                status,
                currentUser,
                isManager,
                pageable
        );

        return ResponseEntity. ok(ApiResponse. success(response));
    }

    // ==================== DEPRECATED ENDPOINTS (Keep for backward compatibility) ====================

    /**
     * @deprecated Use GET /api/billing/invoices? user=me instead
     */
    @Deprecated
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
     * @deprecated Use GET /api/billing/invoices?customerId={id} instead
     */
    @Deprecated
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getCustomerInvoices(
            @PathVariable String customerId
    ) {
        List<InvoiceResponse> response = invoiceService. getCustomerInvoices(customerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * @deprecated Use GET /api/billing/invoices?status={status} instead
     */
    @Deprecated
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesByStatus(
            @PathVariable InvoiceStatus status
    ) {
        List<InvoiceResponse> response = invoiceService.getInvoicesByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== PAY ====================

    /**
     * Pay an invoice (simple button click)
     */
    @PostMapping("/{invoiceId}/pay")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> payInvoice(
            @PathVariable String invoiceId,
            @Valid @RequestBody PayInvoiceRequest request,
            Authentication authentication
    ) {
        String paidBy = authentication. getName();
        InvoiceResponse response = invoiceService.payInvoice(invoiceId, request, paidBy);
        return ResponseEntity. ok(ApiResponse. success("Payment successful!", response));
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat. ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat. ISO.DATE) LocalDate endDate
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
        return ResponseEntity. ok(ApiResponse. success(response));
    }

    // ==================== HELPER METHODS ====================

    private boolean isManagerOrAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_SERVICE_MANAGER") ||
                        role.equals("ROLE_ADMIN") ||
                        role.equals("SERVICE_MANAGER") ||
                        role.equals("ADMIN"));
    }
}