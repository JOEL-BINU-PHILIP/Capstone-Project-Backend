package com.app.booking.controller;

import com.app.booking. dto.request.CancelBookingRequest;
import com.app.booking.dto.request.CreateBookingRequest;
import com. app.booking.dto.request. RateBookingRequest;
import com. app.booking.dto.request.RescheduleBookingRequest;
import com.app.booking.dto.response.ApiResponse;
import com.app. booking.dto.response.BookingResponse;
import com.app. booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org. springframework.data.domain.Page;
import org.springframework.data. domain.Pageable;
import org.springframework.data.web. PageableDefault;
import org. springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework. security.access.prepost.PreAuthorize;
import org. springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bookings/customer")
@RequiredArgsConstructor
public class CustomerBookingController {

    private final BookingService bookingService;

    /**
     * Create a new booking
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            Authentication authentication
    ) {
        String customerId = authentication.getName();

        // Note: In production, fetch customer details from auth service or user context
        // For now, using placeholders - you should integrate with your auth service
        BookingResponse response = bookingService. createBooking(
                request,
                customerId,
                "Customer Name",  // TODO: Get from auth service
                "customer@email.com",  // TODO: Get from auth service
                "1234567890"  // TODO: Get from auth service
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse. success("Booking created successfully", response));
    }

    /**
     * Get all bookings for the logged-in customer
     */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            Authentication authentication
    ) {
        String customerId = authentication.getName();
        List<BookingResponse> bookings = bookingService.getCustomerBookings(customerId);

        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * Get paginated bookings for the logged-in customer
     */
    @GetMapping("/paged")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getMyBookingsPaged(
            Authentication authentication,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        String customerId = authentication.getName();
        Page<BookingResponse> bookings = bookingService.getCustomerBookingsPaged(customerId, pageable);

        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * Get a specific booking by ID
     */
    @GetMapping("/{bookingId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable String bookingId,
            Authentication authentication
    ) {
        // Service layer validates customer ownership
        BookingResponse booking = bookingService.getBookingById(bookingId);

        // Verify ownership
        if (!booking.getCustomerId().equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    /**
     * Reschedule a booking
     */
    @PutMapping("/{bookingId}/reschedule")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> rescheduleBooking(
            @PathVariable String bookingId,
            @Valid @RequestBody RescheduleBookingRequest request,
            Authentication authentication
    ) {
        String customerId = authentication.getName();
        BookingResponse response = bookingService.rescheduleBooking(bookingId, request, customerId);

        return ResponseEntity.ok(ApiResponse.success("Booking rescheduled successfully", response));
    }

    /**
     * Cancel a booking
     */
    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable String bookingId,
            @Valid @RequestBody CancelBookingRequest request,
            Authentication authentication
    ) {
        String customerId = authentication.getName();
        BookingResponse response = bookingService. cancelBookingByCustomer(bookingId, request, customerId);

        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", response));
    }

    /**
     * Generate OTP for service completion verification
     */
    @PostMapping("/{bookingId}/generate-otp")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<String>> generateCompletionOtp(
            @PathVariable String bookingId,
            Authentication authentication
    ) {
        String customerId = authentication.getName();
        String otp = bookingService.generateCompletionOtp(bookingId, customerId);

        return ResponseEntity.ok(ApiResponse.success("OTP generated successfully", otp));
    }

    /**
     * Rate a completed booking
     */
    @PostMapping("/{bookingId}/rate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> rateBooking(
            @PathVariable String bookingId,
            @Valid @RequestBody RateBookingRequest request,
            Authentication authentication
    ) {
        String customerId = authentication.getName();
        BookingResponse response = bookingService.rateBooking(bookingId, request, customerId);

        return ResponseEntity.ok(ApiResponse. success("Rating submitted successfully", response));
    }

    /**
     * Track booking by booking number
     */
    @GetMapping("/track/{bookingNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> trackBooking(
            @PathVariable String bookingNumber,
            Authentication authentication
    ) {
        BookingResponse booking = bookingService. getBookingByNumber(bookingNumber);

        // Verify ownership
        if (!booking.getCustomerId().equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        return ResponseEntity.ok(ApiResponse.success(booking));
    }
}