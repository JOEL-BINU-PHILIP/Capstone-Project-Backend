package com.app.booking.controller;

import com.app.booking.dto.request.CancelBookingRequest;
import com.app.booking.dto.request.RateBookingRequest;
import com.app.booking.dto.request.RescheduleBookingRequest;
import com.app.booking.dto.response.BookingResponse;
import com.app.booking.model.BookingStatus;
import com.app.booking.security.JwtUtil;
import com.app.booking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Part 2: Customer Action Tests
 */
@WebMvcTest(BookingController.class)
class BookingControllerTest_CustomerActions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private JwtUtil jwtUtil;

    private BookingResponse bookingResponse;
    private String authToken = "Bearer test-token";

    @BeforeEach
    void setUp() {
        bookingResponse = BookingResponse.builder()
                .id("booking123")
                .bookingNumber("BK-2026-00001")
                .customerId("customer123")
                .status(BookingStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(jwtUtil.extractUserId(anyString())).thenReturn("customer123");
    }

    // ==================== RESCHEDULE BOOKING TESTS ====================

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void rescheduleBooking_ShouldReturnUpdatedBooking_WhenValid() throws Exception {
        RescheduleBookingRequest request = RescheduleBookingRequest.builder()
                .newScheduledDate(LocalDateTime.now().plusDays(2))
                .reason("Need to reschedule")
                .build();

        when(bookingService.rescheduleBooking(
                eq("booking123"), any(RescheduleBookingRequest.class), eq("customer123")
        )).thenReturn(bookingResponse);

        mockMvc.perform(put("/api/bookings/booking123/reschedule")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Booking rescheduled successfully"));

        verify(bookingService, times(1)).rescheduleBooking(
                eq("booking123"), any(RescheduleBookingRequest.class), eq("customer123")
        );
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void rescheduleBooking_ShouldReturnBadRequest_WhenMissingDate() throws Exception {
        RescheduleBookingRequest request = RescheduleBookingRequest.builder()
                .reason("Need to reschedule")
                .build(); // Missing newScheduledDate

        mockMvc.perform(put("/api/bookings/booking123/reschedule")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).rescheduleBooking(anyString(), any(), anyString());
    }

    @Test
    @WithMockUser(username = "technician", roles = {"TECHNICIAN"})
    void rescheduleBooking_ShouldReturnForbidden_WhenNotCustomer() throws Exception {
        RescheduleBookingRequest request = RescheduleBookingRequest.builder()
                .newScheduledDate(LocalDateTime.now().plusDays(2))
                .build();

        mockMvc.perform(put("/api/bookings/booking123/reschedule")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== CANCEL BOOKING TESTS ====================

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void cancelBooking_ShouldReturnCancelledBooking_WhenCustomer() throws Exception {
        CancelBookingRequest request = CancelBookingRequest.builder()
                .reason("Changed my mind")
                .build();

        bookingResponse.setStatus(BookingStatus.CANCELLED);
        when(bookingService.cancelBookingByCustomer(
                eq("booking123"), any(CancelBookingRequest.class), eq("customer123")
        )).thenReturn(bookingResponse);

        mockMvc.perform(post("/api/bookings/booking123/cancel")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Booking cancelled successfully"));

        verify(bookingService, times(1)).cancelBookingByCustomer(
                eq("booking123"), any(CancelBookingRequest.class), eq("customer123")
        );
    }

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void cancelBooking_ShouldUsesManagerMethod_WhenManager() throws Exception {
        CancelBookingRequest request = CancelBookingRequest.builder()
                .reason("Service unavailable")
                .build();

        when(bookingService.cancelBookingByManager(
                eq("booking123"), any(CancelBookingRequest.class), eq("customer123")
        )).thenReturn(bookingResponse);

        mockMvc.perform(post("/api/bookings/booking123/cancel")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bookingService, times(1)).cancelBookingByManager(
                eq("booking123"), any(CancelBookingRequest.class), eq("customer123")
        );
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void cancelBooking_ShouldReturnBadRequest_WhenMissingReason() throws Exception {
        CancelBookingRequest request = CancelBookingRequest.builder().build();

        mockMvc.perform(post("/api/bookings/booking123/cancel")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== RATE BOOKING TESTS ====================

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void rateBooking_ShouldReturnRatedBooking_WhenValid() throws Exception {
        RateBookingRequest request = RateBookingRequest.builder()
                .rating(5)
                .feedback("Excellent service!")
                .build();

        when(bookingService.rateBooking(
                eq("booking123"), any(RateBookingRequest.class), eq("customer123")
        )).thenReturn(bookingResponse);

        mockMvc.perform(post("/api/bookings/booking123/rate")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Rating submitted successfully"));

        verify(bookingService, times(1)).rateBooking(
                eq("booking123"), any(RateBookingRequest.class), eq("customer123")
        );
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void rateBooking_ShouldReturnBadRequest_WhenRatingOutOfRange() throws Exception {
        RateBookingRequest request = RateBookingRequest.builder()
                .rating(6) // Invalid:  should be 1-5
                .feedback("Great")
                .build();

        mockMvc.perform(post("/api/bookings/booking123/rate")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "technician", roles = {"TECHNICIAN"})
    void rateBooking_ShouldReturnForbidden_WhenNotCustomer() throws Exception {
        RateBookingRequest request = RateBookingRequest.builder()
                .rating(5)
                .build();

        mockMvc.perform(post("/api/bookings/booking123/rate")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== GENERATE OTP TESTS ====================

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void generateOtp_ShouldReturnOtp_WhenValid() throws Exception {
        String otp = "123456";
        when(bookingService.generateCompletionOtp("booking123", "customer123"))
                .thenReturn(otp);

        mockMvc.perform(post("/api/bookings/booking123/generate-otp")
                        .with(csrf())
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$. success").value(true))
                .andExpect(jsonPath("$.message").value("OTP generated successfully"))
                .andExpect(jsonPath("$.data").value("123456"));

        verify(bookingService, times(1)).generateCompletionOtp("booking123", "customer123");
    }

    @Test
    @WithMockUser(username = "technician", roles = {"TECHNICIAN"})
    void generateOtp_ShouldReturnForbidden_WhenNotCustomer() throws Exception {
        mockMvc.perform(post("/api/bookings/booking123/generate-otp")
                        .with(csrf())
                        .header("Authorization", authToken))
                .andExpect(status().isForbidden());

        verify(bookingService, never()).generateCompletionOtp(anyString(), anyString());
    }
}
