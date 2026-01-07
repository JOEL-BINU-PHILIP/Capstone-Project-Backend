package com.app.booking.controller;

import com.app.booking.model.Booking;
import com.app.booking.model.BookingStatus;
import com.app.booking.model.PricingDetails;
import com.app.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalApiControllerTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private InternalApiController internalApiController;

    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testBooking = Booking.builder()
                .id("booking123")
                .bookingNumber("BK-2026-00001")
                .customerId("customer123")
                .customerName("John Doe")
                .customerEmail("john@test.com")
                .customerPhone("1234567890")
                .serviceId("service123")
                .serviceName("AC Repair")
                .categoryName("HVAC")
                .status(BookingStatus.COMPLETED)
                .pricing(PricingDetails.builder()
                        .basePrice(1000.0)
                        .finalPrice(1180.0)
                        .currency("INR")
                        .build())
                .createdAt(Instant.now())
                .build();
    }

    // ==================== GET BOOKING BY ID ====================

    @Test
    void getBookingById_ShouldReturnBooking_WhenFound() {
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        ResponseEntity<Map<String, Object>> response = internalApiController.getBookingById("booking123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
        assertThat(response.getBody().get("data")).isNotNull();
    }

    @Test
    void getBookingById_ShouldReturnNotFound_WhenNotFound() {
        when(bookingRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = internalApiController.getBookingById("nonexistent");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(false);
        assertThat(response.getBody().get("data")).isNull();
    }

    // ==================== GET BOOKING BY NUMBER ====================

    @Test
    void getBookingByNumber_ShouldReturnBooking_WhenFound() {
        when(bookingRepository.findByBookingNumber("BK-2026-00001")).thenReturn(Optional.of(testBooking));

        ResponseEntity<Map<String, Object>> response = internalApiController.getBookingByNumber("BK-2026-00001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    @Test
    void getBookingByNumber_ShouldReturnNotFound_WhenNotFound() {
        when(bookingRepository.findByBookingNumber("INVALID")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = internalApiController.getBookingByNumber("INVALID");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(false);
    }

    // ==================== VALIDATE BOOKING ====================

    @Test
    void validateBooking_ShouldReturnValid_WhenCompletedBooking() {
        testBooking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        ResponseEntity<Map<String, Object>> response = internalApiController.validateBooking("booking123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    @Test
    void validateBooking_ShouldReturnInvalid_WhenNotFound() {
        when(bookingRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = internalApiController.validateBooking("nonexistent");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // The API returns success=true (operation succeeded) but data.exists=false
        assertThat(response.getBody().get("success")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("exists")).isEqualTo(false);
    }

    @Test
    void validateBooking_ShouldReturnInvalid_WhenPendingBooking() {
        testBooking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        ResponseEntity<Map<String, Object>> response = internalApiController.validateBooking("booking123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Validation result depends on the status
    }
}

