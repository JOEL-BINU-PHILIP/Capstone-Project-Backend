package com.app.billing.client;

import com.app.billing.client.fallback.BookingServiceFallback;
import com.app.billing.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web. bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(
        name = "booking-service",
        url = "${services.booking-service.url:http://localhost:8083}",
        configuration = FeignConfig.class,
        fallback = BookingServiceFallback.class
)
public interface BookingServiceClient {

    /**
     * Get booking details by ID
     */
    @GetMapping("/api/internal/bookings/{bookingId}")
    Map<String, Object> getBookingById(@PathVariable("bookingId") String bookingId);

    /**
     * Get booking by booking number
     */
    @GetMapping("/api/internal/bookings/number/{bookingNumber}")
    Map<String, Object> getBookingByNumber(@PathVariable("bookingNumber") String bookingNumber);

    /**
     * Validate if booking exists and can be invoiced
     */
    @GetMapping("/api/internal/bookings/{bookingId}/validate")
    Map<String, Object> validateBooking(@PathVariable("bookingId") String bookingId);

    /**
     * Get booking data for invoice creation
     */
    @GetMapping("/api/internal/bookings/{bookingId}/for-invoice")
    Map<String, Object> getBookingForInvoice(@PathVariable("bookingId") String bookingId);
}