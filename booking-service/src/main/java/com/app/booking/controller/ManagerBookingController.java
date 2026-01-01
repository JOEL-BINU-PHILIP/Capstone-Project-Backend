package com.app.booking.controller;

import com.app.booking.dto.request.AssignTechnicianRequest;
import com.app.booking.dto.request.CancelBookingRequest;
import com.app.booking.dto.response.ApiResponse;
import com. app.booking.dto.response. BookingResponse;
import com. app.booking.dto.response.BookingStatsResponse;
import com.app.booking.model.BookingStatus;
import com.app.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok. extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org. springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core. Authentication;
import org.springframework. web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bookings/manager")
@RequiredArgsConstructor
public class ManagerBookingController {

    private final BookingService bookingService;

    /**
     * Get all bookings (paginated)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getAllBookings(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<BookingResponse> bookings = bookingService.getAllBookingsPaged(pageable);
        return ResponseEntity.ok(ApiResponse. success(bookings));
    }

    /**
     * Get pending bookings (waiting for technician assignment)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getPendingBookings() {
        List<BookingResponse> bookings = bookingService.getPendingBookings();
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * Get bookings by status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByStatus(
            @PathVariable BookingStatus status
    ) {
        List<BookingResponse> bookings = bookingService.getBookingsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * Get a specific booking by ID
     */
    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable String bookingId
    ) {
        BookingResponse booking = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    /**
     * Get booking by booking number
     */
    @GetMapping("/number/{bookingNumber}")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByNumber(
            @PathVariable String bookingNumber
    ) {
        BookingResponse booking = bookingService. getBookingByNumber(bookingNumber);
        return ResponseEntity. ok(ApiResponse.success(booking));
    }

    /**
     * Assign a technician to a booking
     */
    @PostMapping("/{bookingId}/assign")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> assignTechnician(
            @PathVariable String bookingId,
            @Valid @RequestBody AssignTechnicianRequest request,
            Authentication authentication
    ) {
        String managerId = authentication.getName();
        BookingResponse response = bookingService.assignTechnician(bookingId, request, managerId);

        return ResponseEntity.ok(ApiResponse.success("Technician assigned successfully", response));
    }

    /**
     * Reassign a different technician to a booking
     */
    @PostMapping("/{bookingId}/reassign")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> reassignTechnician(
            @PathVariable String bookingId,
            @Valid @RequestBody AssignTechnicianRequest request,
            Authentication authentication
    ) {
        String managerId = authentication.getName();
        BookingResponse response = bookingService. reassignTechnician(bookingId, request, managerId);

        return ResponseEntity.ok(ApiResponse.success("Technician reassigned successfully", response));
    }

    /**
     * Cancel a booking (manager override)
     */
    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable String bookingId,
            @Valid @RequestBody CancelBookingRequest request,
            Authentication authentication
    ) {
        String managerId = authentication.getName();
        BookingResponse response = bookingService. cancelBookingByManager(bookingId, request, managerId);

        return ResponseEntity.ok(ApiResponse.success("Booking cancelled", response));
    }

    /**
     * Get booking statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingStatsResponse>> getBookingStats() {
        BookingStatsResponse stats = bookingService.getBookingStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Search bookings by query (booking number, customer name, service name)
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> searchBookings(
            @RequestParam String query
    ) {
        List<BookingResponse> bookings = bookingService.searchBookings(query);
        return ResponseEntity. ok(ApiResponse.success(bookings));
    }
}