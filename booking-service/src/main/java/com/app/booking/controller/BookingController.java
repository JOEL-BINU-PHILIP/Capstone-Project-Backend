package com.app.booking.controller;

import com.app.booking.dto.request.*;
import com.app.booking.dto. response. ApiResponse;
import com.app.booking. dto.response.BookingResponse;
import com.app.booking.dto.response.BookingStatsResponse;
import com.app.booking.model.BookingStatus;
import com. app.booking.security.JwtUtil;
import com. app.booking.service. BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain. PageRequest;
import org.springframework.data. domain.Pageable;
import org. springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework. http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org. springframework.security.core.Authentication;
import org.springframework.security.core. GrantedAuthority;
import org.springframework.security.core. context.SecurityContextHolder;
import org. springframework.web.bind.annotation.*;

import java.util. List;
import java.util.stream.Collectors;

/**
 * Consolidated Booking Controller
 * Single controller handling all booking operations with role-based access control
 */
@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final JwtUtil jwtUtil;

    // ==================== CREATE BOOKING ====================

    /**
     * Create a new booking (Customer only)
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader. substring(7);
        String customerId = jwtUtil.extractUserId(token);
        String customerName = jwtUtil.extractFullName(token);
        String customerEmail = jwtUtil.extractEmail(token);
        String customerPhone = jwtUtil. extractPhoneNumber(token);

        log.info("Creating booking for customer: {}", customerId);

        BookingResponse booking = bookingService.createBooking(
                request, customerId, customerName, customerEmail, customerPhone
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", booking));
    }

    // ==================== GET BOOKINGS ====================

    /**
     * Get bookings based on user role
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookings(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);
        String userId = jwtUtil. extractUserId(token);
        List<String> roles = getRolesFromAuth();

        List<BookingResponse> bookings;

        if (hasRole(roles, "SERVICE_MANAGER") || hasRole(roles, "ADMIN")) {
            log.info("Manager/Admin {} fetching all bookings", userId);
            bookings = bookingService.getAllBookings();
        } else if (hasRole(roles, "TECHNICIAN")) {
            log. info("Technician {} fetching assigned bookings", userId);
            bookings = bookingService.getTechnicianBookings(userId);
        } else {
            log.info("Customer {} fetching own bookings", userId);
            bookings = bookingService.getCustomerBookings(userId);
        }

        return ResponseEntity. ok(ApiResponse. success(bookings));
    }

    /**
     * Get bookings with pagination
     */
    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getBookingsPaged(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir
    ) {
        String token = authHeader.substring(7);
        String userId = jwtUtil.extractUserId(token);
        List<String> roles = getRolesFromAuth();

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest. of(page, size, sort);

        Page<BookingResponse> bookings;

        if (hasRole(roles, "SERVICE_MANAGER") || hasRole(roles, "ADMIN")) {
            bookings = bookingService.getAllBookingsPaged(pageable);
        } else if (hasRole(roles, "TECHNICIAN")) {
            bookings = bookingService.getTechnicianBookingsPaged(userId, pageable);
        } else {
            bookings = bookingService.getCustomerBookingsPaged(userId, pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * Get all bookings (Manager/Admin only)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings() {
        log.info("Fetching all bookings");
        List<BookingResponse> bookings = bookingService. getAllBookings();
        return ResponseEntity. ok(ApiResponse. success(bookings));
    }

    /**
     * Get all bookings with pagination (Manager/Admin only)
     */
    @GetMapping("/all/paged")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getAllBookingsPaged(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir. equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<BookingResponse> bookings = bookingService.getAllBookingsPaged(pageable);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    // ==================== GET SINGLE BOOKING ====================

    /**
     * Get booking by ID with role-based access
     */
    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable("bookingId") String bookingId,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);
        String userId = jwtUtil. extractUserId(token);
        List<String> roles = getRolesFromAuth();

        log.info("User {} fetching booking {}", userId, bookingId);

        BookingResponse booking = bookingService.getBookingByIdWithAccessCheck(bookingId, userId, roles);

        return ResponseEntity. ok(ApiResponse. success(booking));
    }

    /**
     * Get booking by booking number
     */
    @GetMapping("/number/{bookingNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByNumber(
            @PathVariable("bookingNumber") String bookingNumber,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);
        String userId = jwtUtil. extractUserId(token);
        List<String> roles = getRolesFromAuth();

        log.info("User {} fetching booking by number {}", userId, bookingNumber);

        BookingResponse booking = bookingService. getBookingByNumberWithAccessCheck(bookingNumber, userId, roles);

        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    // ==================== TECHNICIAN SPECIFIC ====================

    /**
     * Get technician's active bookings
     */
    @GetMapping("/technician/active")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getTechnicianActiveBookings(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader. substring(7);
        String technicianId = jwtUtil.extractUserId(token);

        log.info("Technician {} fetching active bookings", technicianId);

        List<BookingResponse> bookings = bookingService.getTechnicianActiveBookings(technicianId);
        return ResponseEntity. ok(ApiResponse. success(bookings));
    }

    // ==================== MANAGER SPECIFIC ====================

    /**
     * Get pending bookings (Manager/Admin only)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getPendingBookings() {
        log. info("Fetching pending bookings");
        List<BookingResponse> bookings = bookingService.getPendingBookings();
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * Get bookings by status (Manager/Admin only)
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByStatus(
            @PathVariable("status") BookingStatus status
    ) {
        log.info("Fetching bookings with status: {}", status);
        List<BookingResponse> bookings = bookingService.getBookingsByStatus(status);
        return ResponseEntity. ok(ApiResponse. success(bookings));
    }

    /**
     * Get booking statistics (Manager/Admin only)
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingStatsResponse>> getBookingStats() {
        log. info("Fetching booking statistics");
        BookingStatsResponse stats = bookingService.getBookingStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Search bookings (Manager/Admin only)
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> searchBookings(
            @RequestParam("query") String query
    ) {
        log.info("Searching bookings with query: {}", query);
        List<BookingResponse> bookings = bookingService. searchBookings(query);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    // ==================== BOOKING ACTIONS - CUSTOMER ====================

    /**
     * Reschedule booking (Customer only - own booking)
     */
    @PutMapping("/{bookingId}/reschedule")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> rescheduleBooking(
            @PathVariable("bookingId") String bookingId,
            @Valid @RequestBody RescheduleBookingRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader. substring(7);
        String customerId = jwtUtil.extractUserId(token);

        log.info("Customer {} rescheduling booking {}", customerId, bookingId);

        BookingResponse booking = bookingService.rescheduleBooking(bookingId, request, customerId);
        return ResponseEntity. ok(ApiResponse. success("Booking rescheduled successfully", booking));
    }

    /**
     * Cancel booking (Customer or Manager)
     */
    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable("bookingId") String bookingId,
            @Valid @RequestBody CancelBookingRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);
        String userId = jwtUtil. extractUserId(token);
        List<String> roles = getRolesFromAuth();

        log.info("User {} cancelling booking {}", userId, bookingId);

        BookingResponse booking;
        if (hasRole(roles, "SERVICE_MANAGER") || hasRole(roles, "ADMIN")) {
            booking = bookingService.cancelBookingByManager(bookingId, request, userId);
        } else {
            booking = bookingService.cancelBookingByCustomer(bookingId, request, userId);
        }

        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", booking));
    }

    /**
     * Rate booking (Customer only - own completed booking)
     */
    @PostMapping("/{bookingId}/rate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> rateBooking(
            @PathVariable("bookingId") String bookingId,
            @Valid @RequestBody RateBookingRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);
        String customerId = jwtUtil. extractUserId(token);

        log.info("Customer {} rating booking {}", customerId, bookingId);

        BookingResponse booking = bookingService.rateBooking(bookingId, request, customerId);
        return ResponseEntity.ok(ApiResponse.success("Rating submitted successfully", booking));
    }

    /**
     * Generate OTP for service completion (Customer only)
     */
    @PostMapping("/{bookingId}/generate-otp")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<String>> generateOtp(
            @PathVariable("bookingId") String bookingId,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);
        String customerId = jwtUtil.extractUserId(token);

        log.info("Customer {} generating OTP for booking {}", customerId, bookingId);

        String otp = bookingService.generateCompletionOtp(bookingId, customerId);
        return ResponseEntity.ok(ApiResponse.success("OTP generated successfully", otp));
    }

    // ==================== BOOKING ACTIONS - TECHNICIAN ====================

    /**
     * Confirm booking (Technician only - assigned booking)
     */
    @PostMapping("/{bookingId}/confirm")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable("bookingId") String bookingId,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);
        String technicianId = jwtUtil.extractUserId(token);

        log.info("Technician {} confirming booking {}", technicianId, bookingId);

        BookingResponse booking = bookingService. confirmBooking(bookingId, technicianId);
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed successfully", booking));
    }

    /**
     * Reject booking (Technician only - assigned booking)
     */
    @PostMapping("/{bookingId}/reject")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<BookingResponse>> rejectBooking(
            @PathVariable("bookingId") String bookingId,
            @Valid @RequestBody RejectBookingRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);
        String technicianId = jwtUtil. extractUserId(token);

        log.info("Technician {} rejecting booking {}", technicianId, bookingId);

        BookingResponse booking = bookingService.rejectBooking(bookingId, technicianId, request. getReason());
        return ResponseEntity.ok(ApiResponse.success("Booking rejected successfully", booking));
    }

    /**
     * Start service (Technician only - assigned booking)
     */
    @PostMapping("/{bookingId}/start")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<BookingResponse>> startService(
            @PathVariable("bookingId") String bookingId,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader. substring(7);
        String technicianId = jwtUtil.extractUserId(token);

        log.info("Technician {} starting service for booking {}", technicianId, bookingId);

        BookingResponse booking = bookingService.startService(bookingId, technicianId);
        return ResponseEntity.ok(ApiResponse.success("Service started successfully", booking));
    }

    /**
     * Complete service (Technician only - with OTP verification)
     */
    @PostMapping("/{bookingId}/complete")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<BookingResponse>> completeService(
            @PathVariable("bookingId") String bookingId,
            @Valid @RequestBody CompleteBookingRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);
        String technicianId = jwtUtil.extractUserId(token);

        log.info("Technician {} completing booking {}", technicianId, bookingId);

        BookingResponse booking = bookingService.completeService(bookingId, request, technicianId);
        return ResponseEntity.ok(ApiResponse.success("Service completed successfully", booking));
    }

    // ==================== BOOKING ACTIONS - MANAGER ====================

    /**
     * Assign technician to booking (Manager only)
     */
    @PostMapping("/{bookingId}/assign")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> assignTechnician(
            @PathVariable("bookingId") String bookingId,
            @Valid @RequestBody AssignTechnicianRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);
        String managerId = jwtUtil.extractUserId(token);

        log.info("Manager {} assigning technician to booking {}", managerId, bookingId);

        BookingResponse booking = bookingService.assignTechnician(bookingId, request, managerId);
        return ResponseEntity.ok(ApiResponse.success("Technician assigned successfully", booking));
    }

    /**
     * Reassign technician to booking (Manager only)
     */
    @PostMapping("/{bookingId}/reassign")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> reassignTechnician(
            @PathVariable("bookingId") String bookingId,
            @Valid @RequestBody AssignTechnicianRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);
        String managerId = jwtUtil. extractUserId(token);

        log.info("Manager {} reassigning technician for booking {}", managerId, bookingId);

        BookingResponse booking = bookingService.reassignTechnician(bookingId, request, managerId);
        return ResponseEntity.ok(ApiResponse.success("Technician reassigned successfully", booking));
    }

    // ==================== HELPER METHODS ====================

    private List<String> getRolesFromAuth() {
        Authentication auth = SecurityContextHolder. getContext().getAuthentication();
        return auth. getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .collect(Collectors. toList());
    }

    private boolean hasRole(List<String> roles, String role) {
        return roles.contains(role);
    }
}