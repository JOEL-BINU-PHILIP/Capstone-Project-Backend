package com.app.booking.service.impl;

import com.app.booking.client.AuthServiceClient;
import com.app. booking.client.CatalogServiceClient;
import com. app.booking.dto.external.ApiResponseWrapper;
import com.app.booking.dto.request.CompleteBookingRequest;
import com.app.booking.dto.response.BookingResponse;
import com.app.booking.exception.InvalidStateException;
import com.app.booking.exception.UnauthorizedException;
import com. app.booking.model.*;
import com.app.booking. repository.BookingRepository;
import com.app.booking.service.EventPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org. junit.jupiter.api.Test;
import org.junit.jupiter. api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org. mockito.Mock;
import org.mockito.junit.jupiter. MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito. ArgumentMatchers.*;
import static org.mockito. Mockito.*;

@ExtendWith(MockitoExtension. class)
class BookingServiceImplTest_TechnicianActions {

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
                .technicianId("tech123")
                .status(BookingStatus.ASSIGNED)
                .createdAt(Instant.now())
                .build();
    }

    // ==================== CONFIRM BOOKING TESTS ====================

    @Test
    void confirmBooking_ShouldConfirmBooking_WhenValid() {
        // Arrange
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponse response = bookingService.confirmBooking("booking123", "tech123");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus. CONFIRMED);
        assertThat(testBooking.getConfirmedAt()).isNotNull();
        verify(bookingRepository, times(1)).save(testBooking);
        verify(eventPublisherService, times(1)).publishBookingEvent(any());
    }

    @Test
    void confirmBooking_ShouldThrowException_WhenNotAssignedToTechnician() {
        // Arrange
        when(bookingRepository.findById("booking123")).thenReturn(Optional. of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.confirmBooking("booking123", "different-tech"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not assigned to you");
    }

    @Test
    void confirmBooking_ShouldThrowException_WhenNotInAssignedStatus() {
        // Arrange
        testBooking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.confirmBooking("booking123", "tech123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("only confirm assigned bookings");
    }

    @Test
    void confirmBooking_ShouldThrowException_WhenTechnicianIdIsNull() {
        // Arrange
        testBooking.setTechnicianId(null);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.confirmBooking("booking123", "tech123"))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ==================== REJECT BOOKING TESTS ====================

    @Test
    void rejectBooking_ShouldRejectAndUnassign_WhenValid() {
        // Arrange
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponse response = bookingService.rejectBooking(
                "booking123", "tech123", "Not available at scheduled time"
        );

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus. REJECTED);
        assertThat(testBooking.getTechnicianId()).isNull();
        assertThat(testBooking.getTechnicianName()).isNull();
        assertThat(testBooking.getTechnicianNotes()).isEqualTo("Not available at scheduled time");
        verify(bookingRepository, times(1)).save(testBooking);
        verify(eventPublisherService, times(1)).publishBookingEvent(any());
    }

    @Test
    void rejectBooking_ShouldThrowException_WhenNotAssignedToTechnician() {
        // Arrange
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.rejectBooking("booking123", "different-tech", "Reason"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectBooking_ShouldThrowException_WhenNotInAssignedStatus() {
        // Arrange
        testBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.rejectBooking("booking123", "tech123", "Reason"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("only reject assigned bookings");
    }

    // ==================== START SERVICE TESTS ====================

    @Test
    void startService_ShouldStartService_WhenValid() {
        // Arrange
        testBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponse response = bookingService. startService("booking123", "tech123");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus. IN_PROGRESS);
        assertThat(testBooking.getStartedAt()).isNotNull();
        verify(bookingRepository, times(1)).save(testBooking);
        verify(eventPublisherService, times(1)).publishBookingEvent(any());
    }

    @Test
    void startService_ShouldThrowException_WhenNotInConfirmedStatus() {
        // Arrange
        testBooking. setStatus(BookingStatus. ASSIGNED);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.startService("booking123", "tech123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("only start confirmed bookings");
    }

    @Test
    void startService_ShouldThrowException_WhenNotAssignedToTechnician() {
        // Arrange
        testBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.startService("booking123", "different-tech"))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ==================== COMPLETE SERVICE TESTS ====================

    @Test
    void completeService_ShouldCompleteService_WhenValidOtp() {
        // Arrange
        testBooking.setStatus(BookingStatus.IN_PROGRESS);
        testBooking.setCompletionOtp("123456");

        CompleteBookingRequest request = CompleteBookingRequest.builder()
                .otp("123456")
                .technicianNotes("Service completed successfully")
                .completionImageUrls(Arrays.asList("http://image1.jpg"))
                .build();

        ApiResponseWrapper<Void> incrementResponse = new ApiResponseWrapper<>(true, "Success", null);
        when(authServiceClient.incrementTechnicianJobs("tech123")).thenReturn(incrementResponse);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponse response = bookingService. completeService("booking123", request, "tech123");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus. COMPLETED);
        assertThat(testBooking.getOtpVerified()).isTrue();
        assertThat(testBooking.getCompletedAt()).isNotNull();
        assertThat(testBooking. getTechnicianNotes()).isEqualTo("Service completed successfully");
        verify(bookingRepository, times(1)).save(testBooking);
        verify(authServiceClient, times(1)).incrementTechnicianJobs("tech123");
        verify(eventPublisherService, times(1)).publishBookingEvent(any());
    }

    @Test
    void completeService_ShouldThrowException_WhenInvalidOtp() {
        // Arrange
        testBooking. setStatus(BookingStatus.IN_PROGRESS);
        testBooking.setCompletionOtp("123456");

        CompleteBookingRequest request = CompleteBookingRequest.builder()
                .otp("wrong-otp")
                .build();

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.completeService("booking123", request, "tech123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Invalid completion OTP");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void completeService_ShouldThrowException_WhenOtpIsNull() {
        // Arrange
        testBooking.setStatus(BookingStatus.IN_PROGRESS);
        testBooking. setCompletionOtp(null);

        CompleteBookingRequest request = CompleteBookingRequest. builder()
                .otp("123456")
                .build();

        when(bookingRepository. findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.completeService("booking123", request, "tech123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Invalid completion OTP");
    }

    @Test
    void completeService_ShouldThrowException_WhenNotInProgress() {
        // Arrange
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setCompletionOtp("123456");

        CompleteBookingRequest request = CompleteBookingRequest.builder()
                .otp("123456")
                .build();

        when(bookingRepository.findById("booking123")).thenReturn(Optional. of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.completeService("booking123", request, "tech123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("only complete in-progress bookings");
    }

    @Test
    void completeService_ShouldStillComplete_WhenIncrementJobsFails() {
        // Arrange
        testBooking.setStatus(BookingStatus.IN_PROGRESS);
        testBooking. setCompletionOtp("123456");

        CompleteBookingRequest request = CompleteBookingRequest.builder()
                .otp("123456")
                .build();

        when(authServiceClient.incrementTechnicianJobs("tech123"))
                .thenThrow(new RuntimeException("Auth service unavailable"));
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation. getArgument(0));

        // Act
        BookingResponse response = bookingService.completeService("booking123", request, "tech123");

        // Assert - Should still complete the booking
        assertThat(response).isNotNull();
        assertThat(response. getStatus()).isEqualTo(BookingStatus.COMPLETED);
        verify(bookingRepository, times(1)).save(testBooking);
    }
}