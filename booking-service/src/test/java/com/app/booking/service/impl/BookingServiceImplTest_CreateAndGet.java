package com.app.booking.service. impl;



import com.app. booking.client.AuthServiceClient;
import com.app. booking.client.CatalogServiceClient;
import com.app.booking.dto.request.CreateBookingRequest;
import com. app.booking.dto.response.BookingResponse;
import com.app.booking.exception.ResourceNotFoundException;
import com.app. booking.exception.UnauthorizedException;
import com. app.booking.model.*;
import com.app.booking. repository.BookingRepository;
import com.app.booking.service.EventPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org. junit.jupiter.api.Test;
import org.junit.jupiter. api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org. mockito.Mock;
import org.mockito.junit.jupiter. MockitoExtension;

import java.time. Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito. Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest_CreateAndGet {

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
    private CreateBookingRequest createRequest;
    private Map<String, Object> serviceResponse;

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
                .status(BookingStatus.PENDING)
                .priority(Priority. NORMAL)
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

        // Mock catalog service response
        Map<String, Object> serviceData = new HashMap<>();
        serviceData.put("serviceName", "AC Repair");
        serviceData.put("categoryName", "HVAC");
        serviceData.put("basePrice", 1000.0);
        serviceData.put("taxPercentage", 18.0);
        serviceData. put("taxAmount", 180.0);
        serviceData. put("discountPercentage", 0.0);
        serviceData.put("discountAmount", 0.0);
        serviceData.put("finalPrice", 1180.0);
        serviceData. put("currency", "INR");
        serviceData.put("estimatedDurationMinutes", 120);

        serviceResponse = new HashMap<>();
        serviceResponse.put("success", true);
        serviceResponse. put("message", "Service found");
        serviceResponse.put("data", serviceData);
    }

    // ==================== CREATE BOOKING TESTS ====================

    @Test
    void createBooking_ShouldCreateAndReturnBooking_WhenValidRequest() {
        // Arrange
        when(catalogServiceClient.getServiceForBooking("service123")).thenReturn(serviceResponse);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId("booking123");
            booking.setBookingNumber("BK-2026-00001");
            booking.setCreatedAt(Instant.now());
            return booking;
        });

        // Act
        BookingResponse response = bookingService.createBooking(
                createRequest, "customer123", "John Doe", "john@test.com", "1234567890"
        );

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("booking123");
        assertThat(response.getCustomerId()).isEqualTo("customer123");
        assertThat(response.getServiceName()).isEqualTo("AC Repair");
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);

        verify(catalogServiceClient, times(1)).getServiceForBooking("service123");
        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(eventPublisherService, times(1)).publishBookingEvent(any());
    }

    @Test
    void createBooking_ShouldUseDefaultValues_WhenCatalogServiceFails() {
        // Arrange
        when(catalogServiceClient. getServiceForBooking("service123")).thenThrow(new RuntimeException("Service unavailable"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponse response = bookingService.createBooking(
                createRequest, "customer123", "John Doe", "john@test.com", "1234567890"
        );

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getServiceName()).isEqualTo("Service"); // Default value
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void createBooking_ShouldSetDefaultPriority_WhenNotProvided() {
        // Arrange
        createRequest.setPriority(null);
        when(catalogServiceClient.getServiceForBooking(anyString())).thenReturn(serviceResponse);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponse response = bookingService.createBooking(
                createRequest, "customer123", "John Doe", "john@test.com", "1234567890"
        );

        // Assert
        assertThat(response. getPriority()).isEqualTo(Priority.NORMAL);
    }

    // ==================== GET BOOKING TESTS ====================

    @Test
    void getBookingById_ShouldReturnBooking_WhenExists() {
        // Arrange
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));

        // Act
        BookingResponse response = bookingService. getBookingById("booking123");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response. getId()).isEqualTo("booking123");
        assertThat(response.getBookingNumber()).isEqualTo("BK-2026-00001");
        verify(bookingRepository, times(1)).findById("booking123");
    }

    @Test
    void getBookingById_ShouldThrowException_WhenNotFound() {
        // Arrange
        when(bookingRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> bookingService.getBookingById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    void getBookingByNumber_ShouldReturnBooking_WhenExists() {
        // Arrange
        when(bookingRepository.findByBookingNumber("BK-2026-00001")).thenReturn(Optional.of(testBooking));

        // Act
        BookingResponse response = bookingService.getBookingByNumber("BK-2026-00001");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getBookingNumber()).isEqualTo("BK-2026-00001");
    }

    // ==================== GET WITH ACCESS CHECK TESTS ====================

    @Test
    void getBookingByIdWithAccessCheck_ShouldReturnBooking_WhenCustomerOwnsIt() {
        // Arrange
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        List<String> roles = Arrays.asList("CUSTOMER");

        // Act
        BookingResponse response = bookingService.getBookingByIdWithAccessCheck(
                "booking123", "customer123", roles
        );

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("booking123");
    }

    @Test
    void getBookingByIdWithAccessCheck_ShouldThrowException_WhenCustomerDoesNotOwnIt() {
        // Arrange
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        List<String> roles = Arrays.asList("CUSTOMER");

        // Act & Assert
        assertThatThrownBy(() -> bookingService.getBookingByIdWithAccessCheck(
                "booking123", "different-customer", roles
        )).isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void getBookingByIdWithAccessCheck_ShouldAllowAccess_WhenManager() {
        // Arrange
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        List<String> roles = Arrays.asList("SERVICE_MANAGER");

        // Act
        BookingResponse response = bookingService.getBookingByIdWithAccessCheck(
                "booking123", "any-user-id", roles
        );

        // Assert
        assertThat(response).isNotNull();
    }

    @Test
    void getBookingByIdWithAccessCheck_ShouldAllowAccess_WhenAssignedTechnician() {
        // Arrange
        testBooking.setTechnicianId("tech123");
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        List<String> roles = Arrays.asList("TECHNICIAN");

        // Act
        BookingResponse response = bookingService.getBookingByIdWithAccessCheck(
                "booking123", "tech123", roles
        );

        // Assert
        assertThat(response).isNotNull();
    }

    @Test
    void getBookingByIdWithAccessCheck_ShouldThrowException_WhenDifferentTechnician() {
        // Arrange
        testBooking.setTechnicianId("tech123");
        when(bookingRepository.findById("booking123")).thenReturn(Optional.of(testBooking));
        List<String> roles = Arrays.asList("TECHNICIAN");

        // Act & Assert
        assertThatThrownBy(() -> bookingService.getBookingByIdWithAccessCheck(
                "booking123", "different-tech", roles
        )).isInstanceOf(UnauthorizedException.class);
    }

    // ==================== GET CUSTOMER BOOKINGS TESTS ====================

    @Test
    void getCustomerBookings_ShouldReturnListOfBookings() {
        // Arrange
        List<Booking> bookings = Arrays.asList(testBooking);
        when(bookingRepository.findByCustomerIdOrderByCreatedAtDesc("customer123"))
                .thenReturn(bookings);

        // Act
        List<BookingResponse> responses = bookingService.getCustomerBookings("customer123");

        // Assert
        assertThat(responses).isNotEmpty();
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCustomerId()).isEqualTo("customer123");
    }

    // ==================== GET TECHNICIAN BOOKINGS TESTS ====================

    @Test
    void getTechnicianBookings_ShouldReturnListOfBookings() {
        // Arrange
        testBooking.setTechnicianId("tech123");
        List<Booking> bookings = Arrays.asList(testBooking);
        when(bookingRepository. findByTechnicianIdOrderByScheduledDateAsc("tech123"))
                .thenReturn(bookings);

        // Act
        List<BookingResponse> responses = bookingService.getTechnicianBookings("tech123");

        // Assert
        assertThat(responses).isNotEmpty();
        assertThat(responses. get(0).getTechnicianId()).isEqualTo("tech123");
    }

    @Test
    void getTechnicianActiveBookings_ShouldReturnOnlyActiveBookings() {
        // Arrange
        Booking assignedBooking = Booking.builder()
                .id("booking1")
                .technicianId("tech123")
                .status(BookingStatus.ASSIGNED)
                .build();

        Booking inProgressBooking = Booking.builder()
                .id("booking2")
                .technicianId("tech123")
                .status(BookingStatus.IN_PROGRESS)
                .build();

        List<Booking> activeBookings = Arrays.asList(assignedBooking, inProgressBooking);
        when(bookingRepository.findByTechnicianIdAndStatusIn(eq("tech123"), anyList()))
                .thenReturn(activeBookings);

        // Act
        List<BookingResponse> responses = bookingService.getTechnicianActiveBookings("tech123");

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses).allMatch(r ->
                r.getStatus() == BookingStatus.ASSIGNED ||
                        r.getStatus() == BookingStatus.IN_PROGRESS
        );
    }

    // ==================== GET ALL BOOKINGS (MANAGER) TESTS ====================

    @Test
    void getAllBookings_ShouldReturnAllBookings() {
        // Arrange
        List<Booking> bookings = Arrays.asList(testBooking);
        when(bookingRepository.findAll()).thenReturn(bookings);

        // Act
        List<BookingResponse> responses = bookingService. getAllBookings();

        // Assert
        assertThat(responses).isNotEmpty();
        verify(bookingRepository, times(1)).findAll();
    }

    @Test
    void getPendingBookings_ShouldReturnOnlyPendingBookings() {
        // Arrange
        List<Booking> pendingBookings = Arrays.asList(testBooking);
        when(bookingRepository.findByStatus(BookingStatus.PENDING)).thenReturn(pendingBookings);

        // Act
        List<BookingResponse> responses = bookingService.getPendingBookings();

        // Assert
        assertThat(responses).allMatch(r -> r.getStatus() == BookingStatus.PENDING);
    }

    @Test
    void getBookingsByStatus_ShouldReturnFilteredBookings() {
        // Arrange
        testBooking.setStatus(BookingStatus.COMPLETED);
        List<Booking> completedBookings = Arrays.asList(testBooking);
        when(bookingRepository.findByStatus(BookingStatus.COMPLETED)).thenReturn(completedBookings);

        // Act
        List<BookingResponse> responses = bookingService.getBookingsByStatus(BookingStatus.COMPLETED);

        // Assert
        assertThat(responses).allMatch(r -> r.getStatus() == BookingStatus.COMPLETED);
    }

    @Test
    void searchBookings_ShouldReturnMatchingBookings() {
        // Arrange
        List<Booking> searchResults = Arrays.asList(testBooking);
        when(bookingRepository.searchBookings("AC Repair")).thenReturn(searchResults);

        // Act
        List<BookingResponse> responses = bookingService.searchBookings("AC Repair");

        // Assert
        assertThat(responses).isNotEmpty();
        verify(bookingRepository, times(1)).searchBookings("AC Repair");
    }
}