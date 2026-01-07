package com.app. booking.service.impl;

import com.app.booking.client. AuthServiceClient;
import com. app.booking.client.CatalogServiceClient;
import com. app.booking.dto.external. ApiResponseWrapper;
import com. app.booking.dto.request. AssignTechnicianRequest;
import com.app.booking.dto.request.CancelBookingRequest;
import com.app.booking.dto.response.BookingResponse;
import com.app.booking.exception.BookingException;
import com.app.booking.exception.InvalidStateException;
import com.app.booking.model.*;
import com.app.booking. repository.BookingRepository;
import com.app.booking.service. EventPublisherService;
import org.junit.jupiter.api. BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito. InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito. ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest_ManagerActions {

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
    private Map<String, Object> technicianValidationResponse;
    private Map<String, Object> technicianDetailsResponse;

    @BeforeEach
    void setUp() {
        testBooking = Booking. builder()
                .id("booking123")
                .bookingNumber("BK-2026-00001")
                .customerId("customer123")
                .status(BookingStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        // Mock technician validation response
        Map<String, Object> validationData = new HashMap<>();
        validationData.put("exists", true);
        validationData.put("approved", true);
        validationData.put("available", true);
        validationData.put("canAssign", true);

        technicianValidationResponse = new HashMap<>();
        technicianValidationResponse.put("success", true);
        technicianValidationResponse.put("data", validationData);

        // Mock technician details response
        Map<String, Object> techData = new HashMap<>();
        techData.put("userId", "tech123");
        techData.put("fullName", "Jane Smith");
        techData.put("email", "jane@test.com");
        techData.put("phoneNumber", "9876543210");

        technicianDetailsResponse = new HashMap<>();
        technicianDetailsResponse.put("success", true);
        technicianDetailsResponse. put("data", techData);
    }

    // ==================== ASSIGN TECHNICIAN TESTS ====================

    @Test
    void assignTechnician_ShouldAssignTechnician_WhenValid() {
        // Arrange
        AssignTechnicianRequest request = AssignTechnicianRequest. builder()
                .technicianId("tech123")
                .build();

        ApiResponseWrapper<Map<String, Object>> validationWrapper =
                new ApiResponseWrapper<>(true, "Valid",
                        (Map<String, Object>) technicianValidationResponse.get("data"));
        ApiResponseWrapper<Map<String, Object>> detailsWrapper =
                new ApiResponseWrapper<>(true, "Found",
                        (Map<String, Object>) technicianDetailsResponse.get("data"));

        when(authServiceClient.validateTechnician("tech123")).thenReturn(validationWrapper);
        when(authServiceClient.getTechnicianByUserId("tech123")).thenReturn(detailsWrapper);
        when(bookingRepository.findById("booking123")).thenReturn(Optional. of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponse response = bookingService.assignTechnician("booking123", request, "manager123");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.ASSIGNED);
        assertThat(testBooking.getTechnicianId()).isEqualTo("tech123");
        assertThat(testBooking.getTechnicianName()).isEqualTo("Jane Smith");
        assertThat(testBooking.getTechnicianPhone()).isEqualTo("9876543210");
        assertThat(testBooking.getAssignedBy()).isEqualTo("manager123");
        assertThat(testBooking.getAssignedAt()).isNotNull();

        verify(bookingRepository, times(1)).save(testBooking);
        verify(eventPublisherService, times(2)).publishBookingEvent(any()); // Customer + Technician
    }

    @Test
    void assignTechnician_ShouldThrowException_WhenTechnicianNotApproved() {
        // Arrange
        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .technicianId("tech123")
                .build();

        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("exists", true);
        invalidData.put("approved", false);
        invalidData.put("canAssign", false);

        ApiResponseWrapper<Map<String, Object>> validationWrapper =
                new ApiResponseWrapper<>(true, "Not approved", invalidData);

        when(authServiceClient.validateTechnician("tech123")).thenReturn(validationWrapper);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.assignTechnician("booking123", request, "manager123"))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("not approved");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void assignTechnician_ShouldThrowException_WhenTechnicianNotAvailable() {
        // Arrange
        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .technicianId("tech123")
                .build();

        Map<String, Object> invalidData = new HashMap<>();
        invalidData. put("exists", true);
        invalidData.put("approved", true);
        invalidData.put("available", false);
        invalidData.put("canAssign", false);

        ApiResponseWrapper<Map<String, Object>> validationWrapper =
                new ApiResponseWrapper<>(true, "Not available", invalidData);

        when(authServiceClient.validateTechnician("tech123")).thenReturn(validationWrapper);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.assignTechnician("booking123", request, "manager123"))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void assignTechnician_ShouldThrowException_WhenAuthServiceUnavailable() {
        // Arrange
        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .technicianId("tech123")
                .build();

        Map<String, Object> fallbackData = new HashMap<>();
        fallbackData.put("fallback", true);
        fallbackData.put("canAssign", false);

        ApiResponseWrapper<Map<String, Object>> fallbackWrapper =
                new ApiResponseWrapper<>(false, "Service unavailable", fallbackData);

        when(authServiceClient.validateTechnician("tech123")).thenReturn(fallbackWrapper);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.assignTechnician("booking123", request, "manager123"))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("Unable to validate technician");
    }

    @Test
    void assignTechnician_ShouldThrowException_WhenBookingNotPendingOrRejected() {
        // Arrange
        testBooking.setStatus(BookingStatus.COMPLETED);
        AssignTechnicianRequest request = AssignTechnicianRequest. builder()
                .technicianId("tech123")
                .build();

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.assignTechnician("booking123", request, "manager123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("only assign technician to pending or rejected bookings");
    }

    // ==================== REASSIGN TECHNICIAN TESTS ====================

    @Test
    void reassignTechnician_ShouldReassignTechnician_WhenValid() {
        // Arrange
        testBooking.setStatus(BookingStatus.ASSIGNED);
        testBooking.setTechnicianId("old-tech");
        testBooking.setConfirmedAt(Instant.now());

        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .technicianId("tech123")
                .build();

        ApiResponseWrapper<Map<String, Object>> validationWrapper =
                new ApiResponseWrapper<>(true, "Valid",
                        (Map<String, Object>) technicianValidationResponse.get("data"));
        ApiResponseWrapper<Map<String, Object>> detailsWrapper =
                new ApiResponseWrapper<>(true, "Found",
                        (Map<String, Object>) technicianDetailsResponse.get("data"));

        when(authServiceClient.validateTechnician("tech123")).thenReturn(validationWrapper);
        when(authServiceClient.getTechnicianByUserId("tech123")).thenReturn(detailsWrapper);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponse response = bookingService.reassignTechnician("booking123", request, "manager123");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus. ASSIGNED);
        assertThat(testBooking.getTechnicianId()).isEqualTo("tech123");
        assertThat(testBooking.getConfirmedAt()).isNull(); // Reset
        verify(bookingRepository, times(1)).save(testBooking);
        verify(eventPublisherService, times(2)).publishBookingEvent(any());
    }

    @Test
    void reassignTechnician_ShouldThrowException_WhenInvalidStatus() {
        // Arrange
        testBooking.setStatus(BookingStatus.COMPLETED);
        AssignTechnicianRequest request = AssignTechnicianRequest. builder()
                .technicianId("tech123")
                .build();

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.reassignTechnician("booking123", request, "manager123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot reassign technician at this stage");
    }

    // ==================== CANCEL BOOKING BY MANAGER TESTS ====================

    @Test
    void cancelBookingByManager_ShouldCancelBooking_WhenValid() {
        // Arrange
        CancelBookingRequest request = CancelBookingRequest.builder()
                .reason("Service unavailable")
                .build();

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponse response = bookingService. cancelBookingByManager("booking123", request, "manager123");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(testBooking. getCancellation()).isNotNull();
        assertThat(testBooking.getCancellation().getCancelledBy()).isEqualTo("manager123");
        assertThat(testBooking.getCancellation().getCancelledByRole()).isEqualTo("SERVICE_MANAGER");
        assertThat(testBooking.getCancellation().getCancellationReason()).isEqualTo("Service unavailable");
        verify(bookingRepository, times(1)).save(testBooking);
        verify(eventPublisherService, times(1)).publishBookingEvent(any());
    }

    @Test
    void cancelBookingByManager_ShouldThrowException_WhenAlreadyCompleted() {
        // Arrange
        testBooking.setStatus(BookingStatus.COMPLETED);
        CancelBookingRequest request = CancelBookingRequest.builder()
                .reason("Cancel")
                .build();

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.cancelBookingByManager("booking123", request, "manager123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot cancel completed or already cancelled bookings");
    }

    @Test
    void cancelBookingByManager_ShouldThrowException_WhenAlreadyCancelled() {
        // Arrange
        testBooking.setStatus(BookingStatus.CANCELLED);
        CancelBookingRequest request = CancelBookingRequest. builder()
                .reason("Cancel")
                .build();

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.cancelBookingByManager("booking123", request, "manager123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("already cancelled");
    }
}