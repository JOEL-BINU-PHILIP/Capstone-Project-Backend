package com.app.booking.controller;

import com.app.booking. dto.request.CompleteBookingRequest;
import com.app.booking.dto.response.ApiResponse;
import com. app.booking.dto.response. BookingResponse;
import com. app.booking.service.BookingService;
import jakarta.validation. Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org. springframework.data.domain.Page;
import org.springframework.data. domain.Pageable;
import org.springframework.data.web. PageableDefault;
import org. springframework.http.ResponseEntity;
import org.springframework. security.access.prepost.PreAuthorize;
import org. springframework.security.core.Authentication;
import org.springframework.web. bind.annotation.*;

import java. util.List;

@Slf4j
@RestController
@RequestMapping("/api/bookings/technician")
@RequiredArgsConstructor
public class TechnicianBookingController {

    private final BookingService bookingService;

    /**
     * Get all bookings assigned to the logged-in technician
     */
    @GetMapping
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyAssignments(
            Authentication authentication
    ) {
        String technicianId = authentication.getName();
        List<BookingResponse> bookings = bookingService.getTechnicianBookings(technicianId);

        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * Get paginated bookings for the logged-in technician
     */
    @GetMapping("/paged")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getMyAssignmentsPaged(
            Authentication authentication,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        String technicianId = authentication.getName();
        Page<BookingResponse> bookings = bookingService.getTechnicianBookingsPaged(technicianId, pageable);

        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * Get active bookings (assigned, confirmed, in-progress)
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getActiveAssignments(
            Authentication authentication
    ) {
        String technicianId = authentication.getName();
        List<BookingResponse> bookings = bookingService.getTechnicianActiveBookings(technicianId);

        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * Get a specific booking by ID
     */
    @GetMapping("/{bookingId}")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable String bookingId,
            Authentication authentication
    ) {
        BookingResponse booking = bookingService.getBookingById(bookingId);

        // Verify assignment
        if (booking.getTechnicianId() == null ||
                !booking.getTechnicianId().equals(authentication.getName())) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("This booking is not assigned to you"));
        }

        return ResponseEntity. ok(ApiResponse.success(booking));
    }

    /**
     * Confirm acceptance of an assigned booking
     */
    @PostMapping("/{bookingId}/confirm")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable String bookingId,
            Authentication authentication
    ) {
        String technicianId = authentication.getName();
        BookingResponse response = bookingService.confirmBooking(bookingId, technicianId);

        return ResponseEntity.ok(ApiResponse.success("Booking confirmed successfully", response));
    }

    /**
     * Reject an assigned booking
     */
    @PostMapping("/{bookingId}/reject")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<BookingResponse>> rejectBooking(
            @PathVariable String bookingId,
            @RequestParam String reason,
            Authentication authentication
    ) {
        String technicianId = authentication.getName();
        BookingResponse response = bookingService.rejectBooking(bookingId, technicianId, reason);

        return ResponseEntity.ok(ApiResponse.success("Booking rejected", response));
    }

    /**
     * Start the service (begin work)
     */
    @PostMapping("/{bookingId}/start")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<BookingResponse>> startService(
            @PathVariable String bookingId,
            Authentication authentication
    ) {
        String technicianId = authentication.getName();
        BookingResponse response = bookingService.startService(bookingId, technicianId);

        return ResponseEntity.ok(ApiResponse.success("Service started", response));
    }

    /**
     * Complete the service (requires OTP from customer)
     */
    @PostMapping("/{bookingId}/complete")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<BookingResponse>> completeService(
            @PathVariable String bookingId,
            @Valid @RequestBody CompleteBookingRequest request,
            Authentication authentication
    ) {
        String technicianId = authentication.getName();
        BookingResponse response = bookingService.completeService(bookingId, request, technicianId);

        return ResponseEntity.ok(ApiResponse.success("Service completed successfully", response));
    }
}