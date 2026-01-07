package com.app.booking.controller;

import com.app.booking.dto.request.*;
import com.app.booking.dto.response.BookingResponse;
import com.app.booking.dto.response.BookingStatsResponse;
import com.app.booking.model.BookingStatus;
import com.app.booking.model.Priority;
import com.app.booking.security.JwtUtil;
import com.app.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private BookingController bookingController;

    private BookingResponse bookingResponse;
    private CreateBookingRequest createRequest;
    private String authToken = "Bearer test-token";

    @BeforeEach
    void setUp() {
        bookingResponse = BookingResponse.builder()
                .id("booking123")
                .bookingNumber("BK-2026-00001")
                .customerId("customer123")
                .customerName("John Doe")
                .customerEmail("john@test.com")
                .customerPhone("1234567890")
                .serviceId("service123")
                .serviceName("AC Repair")
                .categoryName("HVAC")
                .status(BookingStatus.PENDING)
                .priority(Priority.NORMAL)
                .problemDescription("AC not cooling")
                .scheduledDate(LocalDateTime.now().plusDays(1))
                .createdAt(Instant.now())
                .build();

        createRequest = CreateBookingRequest.builder()
                .serviceId("service123")
                .problemDescription("AC not cooling")
                .scheduledDate(LocalDateTime.now().plusDays(1))
                .priority(Priority.NORMAL)
                .addressLine1("123 Main St")
                .city("New York")
                .state("NY")
                .zipCode("10001")
                .build();
    }

    // ==================== CREATE BOOKING TESTS ====================

    @Test
    void createBooking_ShouldReturnBookingId_WhenValidRequest() {
        when(jwtUtil.extractUserId(anyString())).thenReturn("customer123");
        when(jwtUtil.extractFullName(anyString())).thenReturn("John Doe");
        when(jwtUtil.extractEmail(anyString())).thenReturn("john@test.com");
        when(jwtUtil.extractPhoneNumber(anyString())).thenReturn("1234567890");
        when(bookingService.createBooking(
                any(CreateBookingRequest.class),
                anyString(), anyString(), anyString(), anyString()
        )).thenReturn(bookingResponse);

        ResponseEntity<String> response = bookingController.createBooking(createRequest, authToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo("booking123");
        verify(bookingService, times(1)).createBooking(
                any(CreateBookingRequest.class),
                eq("customer123"), eq("John Doe"),
                eq("john@test.com"), eq("1234567890")
        );
    }

    // ==================== GET ALL BOOKINGS TESTS ====================

    @Test
    void getAllBookings_ShouldReturnAllBookings() {
        List<BookingResponse> bookings = Arrays.asList(bookingResponse);
        when(bookingService.getAllBookings()).thenReturn(bookings);

        var response = bookingController.getAllBookings();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
    }

    // ==================== GET PENDING BOOKINGS TESTS ====================

    @Test
    void getPendingBookings_ShouldReturnPendingBookings() {
        List<BookingResponse> bookings = Arrays.asList(bookingResponse);
        when(bookingService.getPendingBookings()).thenReturn(bookings);

        var response = bookingController.getPendingBookings();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(bookingService).getPendingBookings();
    }

    // ==================== GET BOOKINGS BY STATUS TESTS ====================

    @Test
    void getBookingsByStatus_ShouldReturnFilteredBookings() {
        List<BookingResponse> bookings = Arrays.asList(bookingResponse);
        when(bookingService.getBookingsByStatus(any(BookingStatus.class))).thenReturn(bookings);

        var response = bookingController.getBookingsByStatus(BookingStatus.PENDING);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bookingService).getBookingsByStatus(BookingStatus.PENDING);
    }

    // ==================== GET BOOKING STATS TESTS ====================

    @Test
    void getBookingStats_ShouldReturnStats() {
        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put("PENDING", 5L);
        statusCounts.put("COMPLETED", 10L);

        BookingStatsResponse stats = BookingStatsResponse.builder()
                .totalBookings(15L)
                .pendingBookings(5L)
                .completedBookings(10L)
                .bookingsByStatus(statusCounts)
                .build();

        when(bookingService.getBookingStats()).thenReturn(stats);

        var response = bookingController.getBookingStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getTotalBookings()).isEqualTo(15L);
    }

    // ==================== SEARCH BOOKINGS TESTS ====================

    @Test
    void searchBookings_ShouldReturnSearchResults() {
        List<BookingResponse> bookings = Arrays.asList(bookingResponse);
        when(bookingService.searchBookings(anyString())).thenReturn(bookings);

        var response = bookingController.searchBookings("AC");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bookingService).searchBookings("AC");
    }

    // ==================== TECHNICIAN ACTIVE BOOKINGS TESTS ====================

    @Test
    void getTechnicianActiveBookings_ShouldReturnActiveBookings() {
        when(jwtUtil.extractUserId(anyString())).thenReturn("tech123");
        List<BookingResponse> bookings = Arrays.asList(bookingResponse);
        when(bookingService.getTechnicianActiveBookings(anyString())).thenReturn(bookings);

        var response = bookingController.getTechnicianActiveBookings(authToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bookingService).getTechnicianActiveBookings("tech123");
    }

    // ==================== RESCHEDULE BOOKING TESTS ====================

    @Test
    void rescheduleBooking_ShouldReturnUpdatedBooking() {
        when(jwtUtil.extractUserId(anyString())).thenReturn("customer123");
        RescheduleBookingRequest request = new RescheduleBookingRequest();
        request.setNewScheduledDate(LocalDateTime.now().plusDays(2));
        when(bookingService.rescheduleBooking(anyString(), any(), anyString())).thenReturn(bookingResponse);

        var response = bookingController.rescheduleBooking("booking123", request, authToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bookingService).rescheduleBooking("booking123", request, "customer123");
    }

    // ==================== RATE BOOKING TESTS ====================

    @Test
    void rateBooking_ShouldReturnRatedBooking() {
        when(jwtUtil.extractUserId(anyString())).thenReturn("customer123");
        RateBookingRequest request = new RateBookingRequest();
        request.setRating(5);
        request.setFeedback("Great service!");
        when(bookingService.rateBooking(anyString(), any(), anyString())).thenReturn(bookingResponse);

        var response = bookingController.rateBooking("booking123", request, authToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bookingService).rateBooking("booking123", request, "customer123");
    }

    // ==================== GENERATE OTP TESTS ====================

    @Test
    void generateOtp_ShouldReturnOtp() {
        when(jwtUtil.extractUserId(anyString())).thenReturn("customer123");
        when(bookingService.generateCompletionOtp(anyString(), anyString())).thenReturn("123456");

        var response = bookingController.generateOtp("booking123", authToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isEqualTo("123456");
    }

    // ==================== CONFIRM BOOKING TESTS ====================

    @Test
    void confirmBooking_ShouldReturnConfirmedBooking() {
        when(jwtUtil.extractUserId(anyString())).thenReturn("tech123");
        bookingResponse.setStatus(BookingStatus.CONFIRMED);
        when(bookingService.confirmBooking(anyString(), anyString())).thenReturn(bookingResponse);

        var response = bookingController.confirmBooking("booking123", authToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bookingService).confirmBooking("booking123", "tech123");
    }

    // ==================== REJECT BOOKING TESTS ====================

    @Test
    void rejectBooking_ShouldReturnRejectedBooking() {
        when(jwtUtil.extractUserId(anyString())).thenReturn("tech123");
        RejectBookingRequest request = new RejectBookingRequest();
        request.setReason("Not available");
        bookingResponse.setStatus(BookingStatus.REJECTED);
        when(bookingService.rejectBooking(anyString(), anyString(), anyString())).thenReturn(bookingResponse);

        var response = bookingController.rejectBooking("booking123", request, authToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bookingService).rejectBooking("booking123", "tech123", "Not available");
    }

    // ==================== START SERVICE TESTS ====================

    @Test
    void startService_ShouldReturnInProgressBooking() {
        when(jwtUtil.extractUserId(anyString())).thenReturn("tech123");
        bookingResponse.setStatus(BookingStatus.IN_PROGRESS);
        when(bookingService.startService(anyString(), anyString())).thenReturn(bookingResponse);

        var response = bookingController.startService("booking123", authToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bookingService).startService("booking123", "tech123");
    }

    // ==================== COMPLETE SERVICE TESTS ====================

    @Test
    void completeService_ShouldReturnCompletedBooking() {
        when(jwtUtil.extractUserId(anyString())).thenReturn("tech123");
        CompleteBookingRequest request = new CompleteBookingRequest();
        request.setOtp("123456");
        request.setTechnicianNotes("Service completed successfully");
        bookingResponse.setStatus(BookingStatus.COMPLETED);
        when(bookingService.completeService(anyString(), any(), anyString())).thenReturn(bookingResponse);

        var response = bookingController.completeService("booking123", request, authToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bookingService).completeService("booking123", request, "tech123");
    }

    // ==================== ASSIGN TECHNICIAN TESTS ====================

    @Test
    void assignTechnician_ShouldReturnAssignedBooking() {
        when(jwtUtil.extractUserId(anyString())).thenReturn("manager123");
        AssignTechnicianRequest request = new AssignTechnicianRequest();
        request.setTechnicianId("tech123");
        bookingResponse.setStatus(BookingStatus.ASSIGNED);
        when(bookingService.assignTechnician(anyString(), any(), anyString())).thenReturn(bookingResponse);

        var response = bookingController.assignTechnician("booking123", request, authToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bookingService).assignTechnician("booking123", request, "manager123");
    }

    // ==================== REASSIGN TECHNICIAN TESTS ====================

    @Test
    void reassignTechnician_ShouldReturnReassignedBooking() {
        when(jwtUtil.extractUserId(anyString())).thenReturn("manager123");
        AssignTechnicianRequest request = new AssignTechnicianRequest();
        request.setTechnicianId("tech456");
        when(bookingService.reassignTechnician(anyString(), any(), anyString())).thenReturn(bookingResponse);

        var response = bookingController.reassignTechnician("booking123", request, authToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bookingService).reassignTechnician("booking123", request, "manager123");
    }
}

