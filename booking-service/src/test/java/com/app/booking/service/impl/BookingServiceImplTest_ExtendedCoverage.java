package com.app.booking.service.impl;

import com.app.booking.client.AuthServiceClient;
import com.app.booking.client.CatalogServiceClient;
import com.app.booking.dto.request.*;
import com.app.booking.dto.response.BookingResponse;
import com.app.booking.dto.response.BookingStatsResponse;
import com.app.booking.exception.BookingException;
import com.app.booking.exception.InvalidStateException;
import com.app.booking.exception.ResourceNotFoundException;
import com.app.booking.model.*;
import com.app.booking.repository.BookingRepository;
import com.app.booking.service.EventPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest_ExtendedCoverage {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventPublisherService eventPublisherService;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private CatalogServiceClient catalogServiceClient;

    @InjectMocks
    private BookingServiceImpl bookingService;

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
                .technicianId("tech123")
                .technicianName("Jane Smith")
                .technicianPhone("9876543210")
                .status(BookingStatus.PENDING)
                .priority(Priority.NORMAL)
                .problemDescription("AC not cooling")
                .scheduledDate(LocalDateTime.now().plusDays(1))
                .createdAt(Instant.now())
                .pricing(PricingDetails.builder()
                        .basePrice(1000.0)
                        .taxPercentage(18.0)
                        .taxAmount(180.0)
                        .discountPercentage(0.0)
                        .discountAmount(0.0)
                        .finalPrice(1180.0)
                        .currency("INR")
                        .build())
                .serviceAddress(AddressDetails.builder()
                        .addressLine1("123 Main St")
                        .city("New York")
                        .state("NY")
                        .zipCode("10001")
                        .build())
                .build();
    }

    // ==================== PAGED QUERY TESTS ====================

    @Test
    void getCustomerBookingsPaged_ShouldReturnPagedBookings() {
        Page<Booking> bookingPage = new PageImpl<>(List.of(testBooking), PageRequest.of(0, 10), 1);
        when(bookingRepository.findByCustomerId(eq("customer123"), any(Pageable.class))).thenReturn(bookingPage);

        Page<BookingResponse> result = bookingService.getCustomerBookingsPaged("customer123", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(bookingRepository).findByCustomerId(eq("customer123"), any(Pageable.class));
    }

    @Test
    void getTechnicianBookingsPaged_ShouldReturnPagedBookings() {
        Page<Booking> bookingPage = new PageImpl<>(List.of(testBooking), PageRequest.of(0, 10), 1);
        when(bookingRepository.findByTechnicianId(eq("tech123"), any(Pageable.class))).thenReturn(bookingPage);

        Page<BookingResponse> result = bookingService.getTechnicianBookingsPaged("tech123", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(bookingRepository).findByTechnicianId(eq("tech123"), any(Pageable.class));
    }

    @Test
    void getAllBookingsPaged_ShouldReturnAllBookings() {
        Page<Booking> bookingPage = new PageImpl<>(List.of(testBooking), PageRequest.of(0, 10), 1);
        when(bookingRepository.findAll(any(Pageable.class))).thenReturn(bookingPage);

        Page<BookingResponse> result = bookingService.getAllBookingsPaged(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(bookingRepository).findAll(any(Pageable.class));
    }

    // ==================== BOOKING STATS TESTS ====================

    @Test
    void getBookingStats_ShouldReturnCorrectStats() {
        when(bookingRepository.count()).thenReturn(100L);
        when(bookingRepository.countByStatus(BookingStatus.PENDING)).thenReturn(10L);
        when(bookingRepository.countByStatus(BookingStatus.ASSIGNED)).thenReturn(20L);
        when(bookingRepository.countByStatus(BookingStatus.CONFIRMED)).thenReturn(5L);
        when(bookingRepository.countByStatus(BookingStatus.IN_PROGRESS)).thenReturn(15L);
        when(bookingRepository.countByStatus(BookingStatus.COMPLETED)).thenReturn(40L);
        when(bookingRepository.countByStatus(BookingStatus.CANCELLED)).thenReturn(8L);
        when(bookingRepository.countByStatus(BookingStatus.REJECTED)).thenReturn(2L);

        BookingStatsResponse stats = bookingService.getBookingStats();

        assertThat(stats.getTotalBookings()).isEqualTo(100L);
        assertThat(stats.getPendingBookings()).isEqualTo(10L);
        assertThat(stats.getAssignedBookings()).isEqualTo(20L);
        assertThat(stats.getInProgressBookings()).isEqualTo(15L);
        assertThat(stats.getCompletedBookings()).isEqualTo(40L);
        assertThat(stats.getCancelledBookings()).isEqualTo(8L);
        assertThat(stats.getBookingsByStatus()).isNotEmpty();
    }

    // ==================== SEARCH TESTS ====================

    @Test
    void searchBookings_ShouldReturnMatchingBookings() {
        when(bookingRepository.searchBookings("AC")).thenReturn(List.of(testBooking));

        List<BookingResponse> results = bookingService.searchBookings("AC");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getServiceName()).isEqualTo("AC Repair");
        verify(bookingRepository).searchBookings("AC");
    }

    @Test
    void searchBookings_ShouldReturnEmptyList_WhenNoMatches() {
        when(bookingRepository.searchBookings("NonExistent")).thenReturn(Collections.emptyList());

        List<BookingResponse> results = bookingService.searchBookings("NonExistent");

        assertThat(results).isEmpty();
    }

    // ==================== GET BY STATUS TESTS ====================

    @Test
    void getBookingsByStatus_ShouldReturnBookingsWithStatus() {
        when(bookingRepository.findByStatus(BookingStatus.PENDING)).thenReturn(List.of(testBooking));

        List<BookingResponse> results = bookingService.getBookingsByStatus(BookingStatus.PENDING);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void getPendingBookings_ShouldReturnPendingBookings() {
        when(bookingRepository.findByStatus(BookingStatus.PENDING)).thenReturn(List.of(testBooking));

        List<BookingResponse> results = bookingService.getPendingBookings();

        assertThat(results).hasSize(1);
    }

    // ==================== CUSTOMER OPERATIONS EXTENDED TESTS ====================

    @Test
    void getCustomerBookings_ShouldReturnAllCustomerBookings() {
        when(bookingRepository.findByCustomerIdOrderByCreatedAtDesc("customer123"))
                .thenReturn(List.of(testBooking));

        List<BookingResponse> results = bookingService.getCustomerBookings("customer123");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCustomerId()).isEqualTo("customer123");
    }

    // ==================== TECHNICIAN OPERATIONS EXTENDED TESTS ====================

    @Test
    void getTechnicianBookings_ShouldReturnAllTechnicianBookings() {
        testBooking.setTechnicianId("tech123");
        when(bookingRepository.findByTechnicianIdOrderByScheduledDateAsc("tech123"))
                .thenReturn(List.of(testBooking));

        List<BookingResponse> results = bookingService.getTechnicianBookings("tech123");

        assertThat(results).hasSize(1);
    }

    @Test
    void getTechnicianActiveBookings_ShouldReturnOnlyActiveBookings() {
        Booking assignedBooking = createBookingWithStatus(BookingStatus.ASSIGNED);
        Booking confirmedBooking = createBookingWithStatus(BookingStatus.CONFIRMED);
        Booking inProgressBooking = createBookingWithStatus(BookingStatus.IN_PROGRESS);

        when(bookingRepository.findByTechnicianIdAndStatusIn(eq("tech123"), anyList()))
                .thenReturn(List.of(assignedBooking, confirmedBooking, inProgressBooking));

        List<BookingResponse> results = bookingService.getTechnicianActiveBookings("tech123");

        assertThat(results).hasSize(3);
    }

    // ==================== CONFIRM BOOKING EXTENDED TESTS ====================

    @Test
    void confirmBooking_ShouldConfirmAssignedBooking() {
        testBooking.setStatus(BookingStatus.ASSIGNED);
        testBooking.setTechnicianId("tech123");
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse result = bookingService.confirmBooking("booking123", "tech123");

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(eventPublisherService).publishBookingEvent(any());
    }

    @Test
    void confirmBooking_ShouldThrowException_WhenNotAssigned() {
        testBooking.setStatus(BookingStatus.PENDING);
        testBooking.setTechnicianId("tech123");
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        assertThatThrownBy(() -> bookingService.confirmBooking("booking123", "tech123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Can only confirm assigned bookings");
    }

    // ==================== REJECT BOOKING TESTS ====================

    @Test
    void rejectBooking_ShouldRejectAssignedBooking() {
        testBooking.setStatus(BookingStatus.ASSIGNED);
        testBooking.setTechnicianId("tech123");
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse result = bookingService.rejectBooking("booking123", "tech123", "Not available");

        assertThat(result.getStatus()).isEqualTo(BookingStatus.REJECTED);
        verify(eventPublisherService).publishBookingEvent(any());
    }

    @Test
    void rejectBooking_ShouldThrowException_WhenNotAssigned() {
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setTechnicianId("tech123");
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        assertThatThrownBy(() -> bookingService.rejectBooking("booking123", "tech123", "Reason"))
                .isInstanceOf(InvalidStateException.class);
    }

    // ==================== START SERVICE TESTS ====================

    @Test
    void startService_ShouldStartConfirmedBooking() {
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setTechnicianId("tech123");
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse result = bookingService.startService("booking123", "tech123");

        assertThat(result.getStatus()).isEqualTo(BookingStatus.IN_PROGRESS);
        verify(eventPublisherService).publishBookingEvent(any());
    }

    @Test
    void startService_ShouldThrowException_WhenNotConfirmed() {
        testBooking.setStatus(BookingStatus.ASSIGNED);
        testBooking.setTechnicianId("tech123");
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        assertThatThrownBy(() -> bookingService.startService("booking123", "tech123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Can only start confirmed bookings");
    }

    // ==================== COMPLETE SERVICE TESTS ====================

    @Test
    void completeService_ShouldCompleteInProgressBooking_WithValidOTP() {
        testBooking.setStatus(BookingStatus.IN_PROGRESS);
        testBooking.setTechnicianId("tech123");
        testBooking.setCompletionOtp("123456");
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        CompleteBookingRequest request = CompleteBookingRequest.builder()
                .otp("123456")
                .technicianNotes("Work completed successfully")
                .build();

        BookingResponse result = bookingService.completeService("booking123", request, "tech123");

        assertThat(result.getStatus()).isEqualTo(BookingStatus.COMPLETED);
    }

    @Test
    void completeService_ShouldThrowException_WithInvalidOTP() {
        testBooking.setStatus(BookingStatus.IN_PROGRESS);
        testBooking.setTechnicianId("tech123");
        testBooking.setCompletionOtp("123456");
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        CompleteBookingRequest request = CompleteBookingRequest.builder()
                .otp("wrong-otp")
                .build();

        assertThatThrownBy(() -> bookingService.completeService("booking123", request, "tech123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Invalid completion OTP");
    }

    @Test
    void completeService_ShouldThrowException_WhenNotInProgress() {
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setTechnicianId("tech123");
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        CompleteBookingRequest request = CompleteBookingRequest.builder()
                .otp("123456")
                .build();

        assertThatThrownBy(() -> bookingService.completeService("booking123", request, "tech123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Can only complete in-progress bookings");
    }

    // ==================== MANAGER ASSIGN TECHNICIAN TESTS ====================

    @Test
    void assignTechnician_ShouldAssignToTechnicianSuccessfully() {
        testBooking.setStatus(BookingStatus.PENDING);

        // Mock validation response
        Map<String, Object> validationData = new HashMap<>();
        validationData.put("canAssign", true);
        validationData.put("exists", true);
        validationData.put("approved", true);
        validationData.put("available", true);

        // Mock technician details response
        Map<String, Object> techData = new HashMap<>();
        techData.put("userId", "tech123");
        techData.put("fullName", "Jane Smith");
        techData.put("phoneNumber", "9876543210");

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(authServiceClient.validateTechnician("tech123")).thenReturn(
                new com.app.booking.dto.external.ApiResponseWrapper<>(true, "Valid", validationData));
        when(authServiceClient.getTechnicianByUserId("tech123")).thenReturn(
                new com.app.booking.dto.external.ApiResponseWrapper<>(true, "Success", techData));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .technicianId("tech123")
                .build();

        BookingResponse result = bookingService.assignTechnician("booking123", request, "manager123");

        assertThat(result.getStatus()).isEqualTo(BookingStatus.ASSIGNED);
        assertThat(result.getTechnicianId()).isEqualTo("tech123");
        verify(eventPublisherService, times(2)).publishBookingEvent(any()); // Customer and Technician events
    }

    @Test
    void assignTechnician_ShouldThrowException_WhenBookingNotPendingOrRejected() {
        testBooking.setStatus(BookingStatus.ASSIGNED);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .technicianId("tech123")
                .build();

        assertThatThrownBy(() -> bookingService.assignTechnician("booking123", request, "manager123"))
                .isInstanceOf(InvalidStateException.class);
    }

    // ==================== REASSIGN TECHNICIAN TESTS ====================

    @Test
    void reassignTechnician_ShouldReassignSuccessfully() {
        testBooking.setStatus(BookingStatus.ASSIGNED);

        // Mock validation response
        Map<String, Object> validationData = new HashMap<>();
        validationData.put("canAssign", true);
        validationData.put("exists", true);
        validationData.put("approved", true);
        validationData.put("available", true);

        // Mock technician details response
        Map<String, Object> techData = new HashMap<>();
        techData.put("userId", "newTech");
        techData.put("fullName", "New Technician");
        techData.put("phoneNumber", "1112223333");

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(authServiceClient.validateTechnician("newTech")).thenReturn(
                new com.app.booking.dto.external.ApiResponseWrapper<>(true, "Valid", validationData));
        when(authServiceClient.getTechnicianByUserId("newTech")).thenReturn(
                new com.app.booking.dto.external.ApiResponseWrapper<>(true, "Success", techData));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .technicianId("newTech")
                .build();

        BookingResponse result = bookingService.reassignTechnician("booking123", request, "manager123");

        assertThat(result.getTechnicianId()).isEqualTo("newTech");
    }

    @Test
    void reassignTechnician_ShouldWorkForRejectedBooking() {
        testBooking.setStatus(BookingStatus.REJECTED);

        // Mock validation response
        Map<String, Object> validationData = new HashMap<>();
        validationData.put("canAssign", true);
        validationData.put("exists", true);
        validationData.put("approved", true);
        validationData.put("available", true);

        // Mock technician details response
        Map<String, Object> techData = new HashMap<>();
        techData.put("userId", "tech456");
        techData.put("fullName", "Another Tech");
        techData.put("phoneNumber", "4445556666");

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(authServiceClient.validateTechnician("tech456")).thenReturn(
                new com.app.booking.dto.external.ApiResponseWrapper<>(true, "Valid", validationData));
        when(authServiceClient.getTechnicianByUserId("tech456")).thenReturn(
                new com.app.booking.dto.external.ApiResponseWrapper<>(true, "Success", techData));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .technicianId("tech456")
                .build();

        BookingResponse result = bookingService.reassignTechnician("booking123", request, "manager123");

        assertThat(result.getStatus()).isEqualTo(BookingStatus.ASSIGNED);
    }

    // ==================== CANCEL BY MANAGER TESTS ====================

    @Test
    void cancelBookingByManager_ShouldCancelPendingBooking() {
        testBooking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        CancelBookingRequest request = CancelBookingRequest.builder()
                .reason("Customer unavailable")
                .build();

        BookingResponse result = bookingService.cancelBookingByManager("booking123", request, "manager123");

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(eventPublisherService).publishBookingEvent(any());
    }

    @Test
    void cancelBookingByManager_ShouldThrowException_WhenAlreadyCompleted() {
        testBooking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        CancelBookingRequest request = CancelBookingRequest.builder()
                .reason("Test reason")
                .build();

        assertThatThrownBy(() -> bookingService.cancelBookingByManager("booking123", request, "manager123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot cancel completed or already cancelled bookings");
    }

    // ==================== RATE BOOKING EXTENDED TESTS ====================

    @Test
    void rateBooking_ShouldThrowException_WhenAlreadyRated() {
        testBooking.setStatus(BookingStatus.COMPLETED);
        testBooking.setRatingFeedback(RatingFeedback.builder()
                .rating(5)
                .feedback("Great service")
                .ratedAt(Instant.now())
                .build());
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        RateBookingRequest request = RateBookingRequest.builder()
                .rating(4)
                .feedback("New feedback")
                .build();

        assertThatThrownBy(() -> bookingService.rateBooking("booking123", request, "customer123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Booking has already been rated");
    }

    // ==================== GENERATE OTP TESTS ====================

    @Test
    void generateCompletionOtp_ShouldGenerateOTP_WhenInProgress() {
        testBooking.setStatus(BookingStatus.IN_PROGRESS);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        String otp = bookingService.generateCompletionOtp("booking123", "customer123");

        assertThat(otp).isNotNull();
        assertThat(otp).hasSize(6);
        assertThat(otp).matches("\\d{6}");
    }

    @Test
    void generateCompletionOtp_ShouldThrowException_WhenNotInProgress() {
        testBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        assertThatThrownBy(() -> bookingService.generateCompletionOtp("booking123", "customer123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("OTP can only be generated when service is in progress");
    }

    // ==================== CATALOG SERVICE FALLBACK TESTS ====================

    @Test
    void createBooking_ShouldUseFallbackValues_WhenCatalogServiceReturnsNull() {
        when(catalogServiceClient.getServiceForBooking("service123")).thenReturn(null);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId("booking123");
            return booking;
        });

        CreateBookingRequest request = CreateBookingRequest.builder()
                .serviceId("service123")
                .problemDescription("Test")
                .scheduledDate(LocalDateTime.now().plusDays(1))
                .addressLine1("123 St")
                .city("City")
                .state("State")
                .zipCode("12345")
                .build();

        BookingResponse result = bookingService.createBooking(
                request, "customer123", "John Doe", "john@test.com", "1234567890"
        );

        assertThat(result).isNotNull();
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_ShouldHandleFallbackResponse() {
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("fallback", true);
        fallbackResponse.put("message", "Service unavailable");

        when(catalogServiceClient.getServiceForBooking("service123")).thenReturn(fallbackResponse);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId("booking123");
            return booking;
        });

        CreateBookingRequest request = CreateBookingRequest.builder()
                .serviceId("service123")
                .problemDescription("Test")
                .scheduledDate(LocalDateTime.now().plusDays(1))
                .addressLine1("123 St")
                .city("City")
                .state("State")
                .zipCode("12345")
                .build();

        BookingResponse result = bookingService.createBooking(
                request, "customer123", "John Doe", "john@test.com", "1234567890"
        );

        assertThat(result).isNotNull();
    }

    // ==================== HELPER METHODS ====================

    private Booking createBookingWithStatus(BookingStatus status) {
        return Booking.builder()
                .id(UUID.randomUUID().toString())
                .bookingNumber("BK-" + UUID.randomUUID().toString().substring(0, 8))
                .customerId("customer123")
                .technicianId("tech123")
                .status(status)
                .createdAt(Instant.now())
                .build();
    }
}

