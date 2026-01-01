package com. app.billing.client.fallback;

import com.app.billing.client.BookingServiceClient;
import lombok.extern.slf4j. Slf4j;
import org. springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Fallback class for Booking Service when circuit breaker is open
 * or service is unavailable
 */
@Slf4j
@Component
public class BookingServiceFallback implements BookingServiceClient {

    @Override
    public Map<String, Object> getBookingById(String bookingId) {
        log.warn("FALLBACK: Booking service unavailable - getBookingById({})", bookingId);
        return createFallbackResponse("Booking service unavailable");
    }

    @Override
    public Map<String, Object> getBookingByNumber(String bookingNumber) {
        log.warn("FALLBACK: Booking service unavailable - getBookingByNumber({})", bookingNumber);
        return createFallbackResponse("Booking service unavailable");
    }

    @Override
    public Map<String, Object> validateBooking(String bookingId) {
        log.warn("FALLBACK: Booking service unavailable - validateBooking({})", bookingId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Booking service unavailable - fallback response");

        Map<String, Object> data = new HashMap<>();
        data.put("exists", false);
        data.put("completed", false);
        data.put("canInvoice", false);
        data.put("fallback", true);
        response.put("data", data);

        return response;
    }

    @Override
    public Map<String, Object> getBookingForInvoice(String bookingId) {
        log.warn("FALLBACK: Booking service unavailable - getBookingForInvoice({})", bookingId);
        return createFallbackResponse("Booking service unavailable");
    }

    private Map<String, Object> createFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message + " - fallback response");
        response.put("data", null);
        response.put("fallback", true);
        return response;
    }
}