package com.app.booking.controller;

import com.app.booking.dto.request.CompleteBookingRequest;
import com.app.booking.dto.request.RejectBookingRequest;
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
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Part 3: Technician Action Tests
 */
@WebMvcTest(BookingController.class)
class BookingControllerTest_TechnicianActions {

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
                .technicianId("tech123")
                .status(BookingStatus.ASSIGNED)
                .createdAt(Instant.now())
                .build();

        when(jwtUtil.extractUserId(anyString())).thenReturn("tech123");
    }

    // ==================== CONFIRM BOOKING TESTS ====================

    @Test
    @WithMockUser(username = "technician", roles = {"TECHNICIAN"})
    void confirmBooking_ShouldReturnConfirmedBooking_WhenValid() throws Exception {
        bookingResponse.setStatus(BookingStatus.CONFIRMED);
        when(bookingService.confirmBooking("booking123", "tech123"))
                .thenReturn(bookingResponse);

        mockMvc.perform(post("/api/bookings/booking123/confirm")
                        .with(csrf())
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Booking confirmed successfully"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        verify(bookingService, times(1)).confirmBooking("booking123", "tech123");
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void confirmBooking_ShouldReturnForbidden_WhenNotTechnician() throws Exception {
        mockMvc.perform(post("/api/bookings/booking123/confirm")
                        .with(csrf())
                        .header("Authorization", authToken))
                .andExpect(status().isForbidden());

        verify(bookingService, never()).confirmBooking(anyString(), anyString());
    }

    // ==================== REJECT BOOKING TESTS ====================

    @Test
    @WithMockUser(username = "technician", roles = {"TECHNICIAN"})
    void rejectBooking_ShouldReturnRejectedBooking_WhenValid() throws Exception {
        RejectBookingRequest request = RejectBookingRequest.builder()
                .reason("Not available at scheduled time")
                .build();

        bookingResponse.setStatus(BookingStatus.REJECTED);
        when(bookingService.rejectBooking(eq("booking123"), eq("tech123"), anyString()))
                .thenReturn(bookingResponse);

        mockMvc.perform(post("/api/bookings/booking123/reject")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$. message").value("Booking rejected successfully"));

        verify(bookingService, times(1)).rejectBooking(
                eq("booking123"), eq("tech123"), eq("Not available at scheduled time")
        );
    }

    @Test
    @WithMockUser(username = "technician", roles = {"TECHNICIAN"})
    void rejectBooking_ShouldReturnBadRequest_WhenReasonTooShort() throws Exception {
        RejectBookingRequest request = RejectBookingRequest.builder()
                .reason("Too short") // Less than 10 characters
                .build();

        mockMvc.perform(post("/api/bookings/booking123/reject")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== START SERVICE TESTS ====================

    @Test
    @WithMockUser(username = "technician", roles = {"TECHNICIAN"})
    void startService_ShouldReturnInProgressBooking_WhenValid() throws Exception {
        bookingResponse.setStatus(BookingStatus.IN_PROGRESS);
        when(bookingService.startService("booking123", "tech123"))
                .thenReturn(bookingResponse);

        mockMvc.perform(post("/api/bookings/booking123/start")
                        .with(csrf())
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Service started successfully"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        verify(bookingService, times(1)).startService("booking123", "tech123");
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void startService_ShouldReturnForbidden_WhenNotTechnician() throws Exception {
        mockMvc.perform(post("/api/bookings/booking123/start")
                        .with(csrf())
                        .header("Authorization", authToken))
                .andExpect(status().isForbidden());
    }

    // ==================== COMPLETE SERVICE TESTS ====================

    @Test
    @WithMockUser(username = "technician", roles = {"TECHNICIAN"})
    void completeService_ShouldReturnCompletedBooking_WhenValid() throws Exception {
        CompleteBookingRequest request = CompleteBookingRequest.builder()
                .otp("123456")
                .technicianNotes("Service completed successfully")
                .completionImageUrls(Arrays.asList("http://image1.jpg", "http://image2.jpg"))
                .build();

        bookingResponse.setStatus(BookingStatus.COMPLETED);
        when(bookingService.completeService(
                eq("booking123"), any(CompleteBookingRequest.class), eq("tech123")
        )).thenReturn(bookingResponse);

        mockMvc.perform(post("/api/bookings/booking123/complete")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Service completed successfully"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(bookingService, times(1)).completeService(
                eq("booking123"), any(CompleteBookingRequest.class), eq("tech123")
        );
    }

    @Test
    @WithMockUser(username = "technician", roles = {"TECHNICIAN"})
    void completeService_ShouldReturnBadRequest_WhenMissingOtp() throws Exception {
        CompleteBookingRequest request = CompleteBookingRequest.builder()
                .technicianNotes("Completed")
                .build(); // Missing OTP

        mockMvc.perform(post("/api/bookings/booking123/complete")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).completeService(anyString(), any(), anyString());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void completeService_ShouldReturnForbidden_WhenNotTechnician() throws Exception {
        CompleteBookingRequest request = CompleteBookingRequest.builder()
                .otp("123456")
                .build();

        mockMvc.perform(post("/api/bookings/booking123/complete")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

}
