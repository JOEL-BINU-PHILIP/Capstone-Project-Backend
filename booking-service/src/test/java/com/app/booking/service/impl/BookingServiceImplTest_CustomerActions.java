package com.app.booking.service.impl;

import com.app.booking.client.AuthServiceClient;
import com.app.booking.client.CatalogServiceClient;
import com.app.booking.dto.request.*;
import com.app.booking. dto.response.BookingResponse;
import com.app.booking. exception.InvalidStateException;
import com.app. booking.exception.UnauthorizedException;
import com.app.booking.model.*;
import com.app.booking. repository.BookingRepository;
import com.app.booking.service. EventPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api. Test;
import org.junit. jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit. jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java. util. Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito. ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest_CustomerActions {

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
                .status(BookingStatus.PENDING)
                .scheduledDate(LocalDateTime.now().plusDays(1))
                .createdAt(Instant. now())
                .build();
    }

    // ==================== RESCHEDULE BOOKING TESTS ====================

    @Test
    void rescheduleBooking_ShouldUpdateScheduledDate_WhenValid() {
        // Arrange
        LocalDateTime newDate = LocalDateTime.now().plusDays(3);
        RescheduleBookingRequest request = RescheduleBookingRequest.builder()
                .newScheduledDate(newDate)
                .reason("Need to reschedule")
                .build();

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponse response = bookingService.rescheduleBooking("booking123", request, "customer123");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getScheduledDate()).isEqualTo(newDate);
        verify(bookingRepository, times(1)).save(testBooking);
        verify(eventPublisherService, times(1)).publishBookingEvent(any());
    }

    @Test
    void rescheduleBooking_ShouldThrowException_WhenNotOwner() {
        // Arrange
        RescheduleBookingRequest request = RescheduleBookingRequest.builder()
                .newScheduledDate(LocalDateTime. now().plusDays(3))
                .build();

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.rescheduleBooking("booking123", request, "different-customer"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void rescheduleBooking_ShouldThrowException_WhenInProgress() {
        // Arrange
        testBooking.setStatus(BookingStatus.IN_PROGRESS);
        RescheduleBookingRequest request = RescheduleBookingRequest.builder()
                .newScheduledDate(LocalDateTime.now().plusDays(3))
                .build();

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.rescheduleBooking("booking123", request, "customer123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot reschedule");
    }

    // ==================== CANCEL BOOKING TESTS ====================

    @Test
    void cancelBookingByCustomer_ShouldCancelBooking_WhenValid() {
        // Arrange
        CancelBookingRequest request = CancelBookingRequest.builder()
                .reason("Changed my mind")
                .build();

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponse response = bookingService. cancelBookingByCustomer("booking123", request, "customer123");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response. getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(testBooking.getCancellation()).isNotNull();
        assertThat(testBooking.getCancellation().getCancelledByRole()).isEqualTo("CUSTOMER");
        verify(bookingRepository, times(1)).save(testBooking);
    }

    @Test
    void cancelBookingByCustomer_ShouldThrowException_WhenAlreadyCompleted() {
        // Arrange
        testBooking.setStatus(BookingStatus.COMPLETED);
        CancelBookingRequest request = CancelBookingRequest. builder()
                .reason("Cancel")
                .build();

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.cancelBookingByCustomer("booking123", request, "customer123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("already completed");
    }

    @Test
    void cancelBookingByCustomer_ShouldThrowException_WhenInProgress() {
        // Arrange
        testBooking.setStatus(BookingStatus.IN_PROGRESS);
        CancelBookingRequest request = CancelBookingRequest.builder()
                .reason("Cancel")
                .build();

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.cancelBookingByCustomer("booking123", request, "customer123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot cancel booking once service has started");
    }

    // ==================== RATE BOOKING TESTS ====================

    @Test
    void rateBooking_ShouldAddRating_WhenValidAndCompleted() {
        // Arrange
        testBooking.setStatus(BookingStatus.COMPLETED);
        RateBookingRequest request = RateBookingRequest.builder()
                .rating(5)
                .feedback("Excellent service!")
                .build();

        when(bookingRepository. findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponse response = bookingService.rateBooking("booking123", request, "customer123");

        // Assert
        assertThat(response).isNotNull();
        assertThat(testBooking.getRatingFeedback()).isNotNull();
        assertThat(testBooking.getRatingFeedback().getRating()).isEqualTo(5);
        assertThat(testBooking.getRatingFeedback().getFeedback()).isEqualTo("Excellent service!");
        verify(bookingRepository, times(1)).save(testBooking);
    }

    @Test
    void rateBooking_ShouldThrowException_WhenNotCompleted() {
        // Arrange
        testBooking.setStatus(BookingStatus.PENDING);
        RateBookingRequest request = RateBookingRequest.builder()
                .rating(5)
                .build();

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.rateBooking("booking123", request, "customer123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("only rate completed bookings");
    }

    @Test
    void rateBooking_ShouldThrowException_WhenAlreadyRated() {
        // Arrange
        testBooking.setStatus(BookingStatus. COMPLETED);
        testBooking.setRatingFeedback(RatingFeedback.builder()
                .rating(4)
                .ratedAt(Instant.now())
                .build());

        RateBookingRequest request = RateBookingRequest.builder()
                .rating(5)
                .build();

        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.rateBooking("booking123", request, "customer123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("already been rated");
    }

    // ==================== GENERATE OTP TESTS ====================

    @Test
    void generateCompletionOtp_ShouldGenerateAndReturnOtp_WhenInProgress() {
        // Arrange
        testBooking.setStatus(BookingStatus.IN_PROGRESS);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String otp = bookingService.generateCompletionOtp("booking123", "customer123");

        // Assert
        assertThat(otp).isNotNull();
        assertThat(otp).hasSize(6);
        assertThat(otp).matches("\\d{6}"); // 6 digits
        assertThat(testBooking.getCompletionOtp()).isEqualTo(otp);
        verify(bookingRepository, times(1)).save(testBooking);
    }

    @Test
    void generateCompletionOtp_ShouldThrowException_WhenNotInProgress() {
        // Arrange
        testBooking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.generateCompletionOtp("booking123", "customer123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("in progress");
    }

    @Test
    void generateCompletionOtp_ShouldThrowException_WhenNotOwner() {
        // Arrange
        testBooking.setStatus(BookingStatus.IN_PROGRESS);
        when(bookingRepository.findById("booking123")).thenReturn(Optional. of(testBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.generateCompletionOtp("booking123", "different-customer"))
                .isInstanceOf(UnauthorizedException.class);
    }
}