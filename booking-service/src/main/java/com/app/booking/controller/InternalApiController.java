package com.app.booking.controller;

import com.app.booking.model. Booking;
import com.app. booking.model.BookingStatus;
import com.app.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org. springframework.http.ResponseEntity;
import org.springframework. web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Internal APIs for inter-service communication.
 * These endpoints are called by other microservices (Billing Service).
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalApiController {

    private final BookingRepository bookingRepository;

    // ==================== BOOKING APIs ====================

    /**
     * Get booking details by ID
     * Called by:  Billing Service (when creating invoice)
     */
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<Map<String, Object>> getBookingById(
            @PathVariable String bookingId
    ) {
        log.debug("Internal API: Getting booking by ID: {}", bookingId);

        Optional<Booking> bookingOpt = bookingRepository. findById(bookingId);

        Map<String, Object> response = new HashMap<>();

        if (bookingOpt. isEmpty()) {
            response.put("success", false);
            response.put("message", "Booking not found");
            response.put("data", null);
            return ResponseEntity.ok(response);
        }

        Booking booking = bookingOpt.get();
        Map<String, Object> bookingData = buildBookingData(booking);

        response.put("success", true);
        response.put("message", "Booking found");
        response.put("data", bookingData);

        return ResponseEntity.ok(response);
    }

    /**
     * Get booking by booking number
     * Called by:  Billing Service
     */
    @GetMapping("/bookings/number/{bookingNumber}")
    public ResponseEntity<Map<String, Object>> getBookingByNumber(
            @PathVariable String bookingNumber
    ) {
        log.debug("Internal API: Getting booking by number: {}", bookingNumber);

        Optional<Booking> bookingOpt = bookingRepository.findByBookingNumber(bookingNumber);

        Map<String, Object> response = new HashMap<>();

        if (bookingOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Booking not found");
            response.put("data", null);
            return ResponseEntity. ok(response);
        }

        Booking booking = bookingOpt.get();
        Map<String, Object> bookingData = buildBookingData(booking);

        response.put("success", true);
        response.put("message", "Booking found");
        response.put("data", bookingData);

        return ResponseEntity. ok(response);
    }

    /**
     * Validate if booking exists and can be invoiced
     * Called by:  Billing Service
     */
    @GetMapping("/bookings/{bookingId}/validate")
    public ResponseEntity<Map<String, Object>> validateBooking(
            @PathVariable String bookingId
    ) {
        log.debug("Internal API: Validating booking: {}", bookingId);

        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> result = new HashMap<>();

        if (bookingOpt.isEmpty()) {
            result.put("exists", false);
            result.put("completed", false);
            result.put("canInvoice", false);
            response.put("success", true);
            response.put("message", "Booking not found");
            response.put("data", result);
            return ResponseEntity.ok(response);
        }

        Booking booking = bookingOpt.get();
        boolean isCompleted = booking.getStatus() == BookingStatus.COMPLETED;

        result.put("exists", true);
        result.put("status", booking.getStatus().name());
        result.put("completed", isCompleted);
        result.put("canInvoice", isCompleted);
        result.put("bookingId", booking.getId());
        result.put("bookingNumber", booking.getBookingNumber());

        response.put("success", true);
        response.put("message", "Booking validation complete");
        response.put("data", result);

        return ResponseEntity.ok(response);
    }

    /**
     * Get booking data specifically for invoice creation
     * Called by: Billing Service (includes all info needed for invoice)
     */
    @GetMapping("/bookings/{bookingId}/for-invoice")
    public ResponseEntity<Map<String, Object>> getBookingForInvoice(
            @PathVariable String bookingId
    ) {
        log.debug("Internal API: Getting booking for invoice: {}", bookingId);

        Optional<Booking> bookingOpt = bookingRepository. findById(bookingId);

        Map<String, Object> response = new HashMap<>();

        if (bookingOpt. isEmpty()) {
            response.put("success", false);
            response.put("message", "Booking not found");
            response.put("data", null);
            return ResponseEntity.ok(response);
        }

        Booking booking = bookingOpt.get();

        // Only completed bookings can be invoiced
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            response.put("success", false);
            response.put("message", "Booking is not completed.  Current status: " + booking.getStatus());
            response.put("data", null);
            return ResponseEntity.ok(response);
        }

        Map<String, Object> invoiceData = new HashMap<>();

        // Booking reference
        invoiceData.put("bookingId", booking.getId());
        invoiceData.put("bookingNumber", booking.getBookingNumber());

        // Customer info
        invoiceData.put("customerId", booking.getCustomerId());
        invoiceData.put("customerName", booking.getCustomerName());
        invoiceData. put("customerEmail", booking.getCustomerEmail());
        invoiceData.put("customerPhone", booking.getCustomerPhone());

        // Service info
        invoiceData.put("serviceId", booking.getServiceId());
        invoiceData.put("serviceName", booking.getServiceName());
        invoiceData. put("categoryName", booking.getCategoryName());

        // Technician info
        invoiceData. put("technicianId", booking.getTechnicianId());
        invoiceData.put("technicianName", booking.getTechnicianName());

        // Pricing (from booking snapshot) - with null checks
        if (booking.getPricing() != null) {
            invoiceData.put("basePrice", booking.getPricing().getBasePrice());
            invoiceData.put("taxAmount", booking.getPricing().getTaxAmount());
            invoiceData.put("discountAmount", booking.getPricing().getDiscountAmount());
            invoiceData. put("finalPrice", booking.getPricing().getFinalPrice());
            invoiceData.put("currency", booking.getPricing().getCurrency());

            // These might not exist in your current model - use defaults
            // If you added taxPercentage and discountPercentage to PricingDetails:
            // invoiceData.put("taxPercentage", booking.getPricing().getTaxPercentage());
            // invoiceData.put("discountPercentage", booking. getPricing().getDiscountPercentage());

            // Calculate percentages from amounts if not stored
            Double basePrice = booking.getPricing().getBasePrice();
            Double taxAmount = booking.getPricing().getTaxAmount();
            Double discountAmount = booking.getPricing().getDiscountAmount();

            if (basePrice != null && basePrice > 0) {
                double taxPercentage = (taxAmount != null) ? (taxAmount / basePrice) * 100 : 0;
                double discountPercentage = (discountAmount != null) ? (discountAmount / basePrice) * 100 : 0;
                invoiceData.put("taxPercentage", taxPercentage);
                invoiceData.put("discountPercentage", discountPercentage);
            } else {
                invoiceData. put("taxPercentage", 0.0);
                invoiceData.put("discountPercentage", 0.0);
            }
        } else {
            // Default pricing if not set
            invoiceData.put("basePrice", 0.0);
            invoiceData.put("taxPercentage", 18.0);  // Default tax
            invoiceData.put("taxAmount", 0.0);
            invoiceData.put("discountPercentage", 0.0);
            invoiceData. put("discountAmount", 0.0);
            invoiceData.put("finalPrice", 0.0);
            invoiceData.put("currency", "INR");
        }

        // Address
        if (booking.getServiceAddress() != null) {
            String address = buildAddressString(booking);
            invoiceData.put("serviceAddress", address);
        }

        // Dates
        invoiceData.put("scheduledDate", booking.getScheduledDate());
        invoiceData.put("completedAt", booking.getCompletedAt());

        response.put("success", true);
        response.put("message", "Booking data for invoice retrieved");
        response.put("data", invoiceData);

        return ResponseEntity.ok(response);
    }

    // ==================== HELPER METHODS ====================

    private Map<String, Object> buildBookingData(Booking booking) {
        Map<String, Object> data = new HashMap<>();

        data.put("id", booking.getId());
        data.put("bookingNumber", booking.getBookingNumber());
        data.put("status", booking. getStatus().name());

        // Customer info
        data. put("customerId", booking.getCustomerId());
        data.put("customerName", booking.getCustomerName());
        data.put("customerEmail", booking.getCustomerEmail());
        data.put("customerPhone", booking.getCustomerPhone());

        // Service info
        data.put("serviceId", booking.getServiceId());
        data.put("serviceName", booking.getServiceName());
        data.put("categoryName", booking.getCategoryName());

        // Technician info
        data. put("technicianId", booking.getTechnicianId());
        data.put("technicianName", booking.getTechnicianName());
        data.put("technicianPhone", booking.getTechnicianPhone());

        // Problem description
        data.put("problemDescription", booking.getProblemDescription());
        data.put("priority", booking.getPriority() != null ? booking.getPriority().name() : null);

        // Pricing - with null checks
        if (booking.getPricing() != null) {
            Map<String, Object> pricing = new HashMap<>();
            pricing. put("basePrice", booking.getPricing().getBasePrice());
            pricing.put("taxAmount", booking.getPricing().getTaxAmount());
            pricing.put("discountAmount", booking.getPricing().getDiscountAmount());
            pricing.put("finalPrice", booking.getPricing().getFinalPrice());
            pricing.put("currency", booking.getPricing().getCurrency());
            data.put("pricing", pricing);
        }

        // Address
        if (booking.getServiceAddress() != null) {
            data.put("serviceAddress", buildAddressString(booking));
        }

        // Dates
        data.put("scheduledDate", booking.getScheduledDate());
        data.put("assignedAt", booking.getAssignedAt());
        data.put("confirmedAt", booking.getConfirmedAt());
        data.put("startedAt", booking.getStartedAt());
        data.put("completedAt", booking.getCompletedAt());
        data.put("createdAt", booking.getCreatedAt());

        // Rating
        if (booking.getRatingFeedback() != null) {
            Map<String, Object> rating = new HashMap<>();
            rating.put("rating", booking.getRatingFeedback().getRating());
            rating. put("feedback", booking.getRatingFeedback().getFeedback());
            rating.put("ratedAt", booking.getRatingFeedback().getRatedAt());
            data.put("ratingFeedback", rating);
        }

        return data;
    }

    private String buildAddressString(Booking booking) {
        if (booking.getServiceAddress() == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        if (booking.getServiceAddress().getAddressLine1() != null) {
            sb.append(booking. getServiceAddress().getAddressLine1());
        }
        if (booking.getServiceAddress().getAddressLine2() != null && ! booking.getServiceAddress().getAddressLine2().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(booking.getServiceAddress().getAddressLine2());
        }
        if (booking. getServiceAddress().getCity() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(booking.getServiceAddress().getCity());
        }
        if (booking.getServiceAddress().getState() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(booking.getServiceAddress().getState());
        }
        if (booking.getServiceAddress().getZipCode() != null) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(booking.getServiceAddress().getZipCode());
        }

        return sb.toString();
    }
}