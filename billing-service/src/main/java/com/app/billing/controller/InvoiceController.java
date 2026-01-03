package com.app.billing. controller;

import com.app.billing. dto.request.PayInvoiceRequest;
import com.app. billing.dto.response.ApiResponse;
import com.app.billing. dto.response.InvoiceResponse;
import com.app. billing.dto.response.RevenueReportResponse;
import com.app.billing.model.InvoiceStatus;
import com. app.billing.security.JwtUtil;
import com.app.billing.service. InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data. domain.Sort;
import org.springframework. data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org. springframework.security.core.Authentication;
import org.springframework.security.core. GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java. util.List;

@Slf4j
@RestController
@RequestMapping("/api/billing/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final JwtUtil jwtUtil;

    // ==================== READ ====================

    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(
            @PathVariable String invoiceId,
            Authentication authentication
    ) {
        InvoiceResponse response = invoiceService.getInvoiceById(invoiceId);
        return ResponseEntity. ok(ApiResponse. success(response));
    }

    @GetMapping("/number/{invoiceNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByNumber(
            @PathVariable String invoiceNumber
    ) {
        InvoiceResponse response = invoiceService. getInvoiceByNumber(invoiceNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByBookingId(
            @PathVariable String bookingId
    ) {
        InvoiceResponse response = invoiceService. getInvoiceByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== LIST ====================

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getInvoices(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) InvoiceStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication
    ) {
        // Extract userId from JWT token
        String token = authHeader.substring(7);
        String currentUserId = jwtUtil.extractUserId(token);
        String currentUsername = authentication.getName();
        boolean isManager = isManagerOrAdmin(authentication);

        log.debug("Get invoices - user param: {}, currentUserId: {}, currentUsername: {}, isManager: {}",
                user, currentUserId, currentUsername, isManager);

        String effectiveCustomerId = null;

        if ("me".equalsIgnoreCase(user)) {
            // Use the userId from JWT, not the username
            effectiveCustomerId = currentUserId;
            log.debug("User requested 'me' - using userId:  {}", effectiveCustomerId);
        } else if (customerId != null) {
            if (! isManager) {
                log.warn("Customer {} attempted to view invoices for customer {}", currentUserId, customerId);
                effectiveCustomerId = currentUserId;
            } else {
                effectiveCustomerId = customerId;
            }
        } else if (! isManager) {
            effectiveCustomerId = currentUserId;
        }

        log.debug("Querying invoices with effectiveCustomerId:  {}, status: {}", effectiveCustomerId, status);

        Page<InvoiceResponse> response = invoiceService. getInvoices(
                effectiveCustomerId,
                status,
                currentUserId,
                isManager,
                pageable
        );

        log.debug("Found {} invoices", response.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== PAY ====================

    @PostMapping("/{invoiceId}/pay")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> payInvoice(
            @PathVariable String invoiceId,
            @Valid @RequestBody PayInvoiceRequest request,
            Authentication authentication
    ) {
        String paidBy = authentication.getName();
        InvoiceResponse response = invoiceService.payInvoice(invoiceId, request, paidBy);
        return ResponseEntity. ok(ApiResponse. success("Payment successful!", response));
    }

    // ==================== CANCEL ====================

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

    @GetMapping("/reports/revenue")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat. ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat. ISO.DATE) LocalDate endDate
    ) {
        RevenueReportResponse response = invoiceService.getRevenueReport(startDate, endDate);
        return ResponseEntity. ok(ApiResponse. success(response));
    }

    // ==================== SEARCH ====================

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> searchInvoices(
            @RequestParam String query
    ) {
        List<InvoiceResponse> response = invoiceService.searchInvoices(query);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== HELPER METHODS ====================

    private boolean isManagerOrAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_SERVICE_MANAGER") ||
                        role. equals("ROLE_ADMIN") ||
                        role.equals("SERVICE_MANAGER") ||
                        role. equals("ADMIN"));
    }
}