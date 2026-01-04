package com.app.booking.service.impl;

import com.app.booking.dto.response.*;
import com.app.booking. model.*;
import com.app.booking. repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org. junit.jupiter.api.Test;
import org.junit.jupiter. api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org. mockito.Mock;
import org.mockito.junit.jupiter. MockitoExtension;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito. Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest_GetRequests {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private List<Booking> testBookings;
    private LocalDate today;
    private Instant todayStart;
    private Instant todayEnd;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        todayStart = today. atStartOfDay(ZoneId.systemDefault()).toInstant();
        todayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        testBookings = new ArrayList<>();

        // Create various bookings
        testBookings. add(createBooking("booking1", BookingStatus.PENDING, "HVAC", null, todayStart));
        testBookings.add(createBooking("booking2", BookingStatus.ASSIGNED, "Plumbing", null, todayStart));
        testBookings.add(createBooking("booking3", BookingStatus. COMPLETED, "HVAC", 5, todayStart. minus(Duration.ofHours(24))));
        testBookings. add(createBooking("booking4", BookingStatus.COMPLETED, "Electrical", 4, todayStart.minus(Duration.ofDays(2))));
        testBookings. add(createBooking("booking5", BookingStatus.CANCELLED, "Plumbing", null, todayStart.minus(Duration. ofDays(5))));
    }

    private Booking createBooking(String id, BookingStatus status, String category, Integer rating, Instant createdAt) {
        Booking booking = Booking.builder()
                .id(id)
                .bookingNumber("BK-2026-" + id)
                .status(status)
                .categoryName(category)
                .createdAt(createdAt)
                .build();

        if (status == BookingStatus.COMPLETED) {
            booking.setCompletedAt(createdAt. plus(Duration.ofHours(24)));
            if (rating != null) {
                booking.setRatingFeedback(RatingFeedback. builder()
                        .rating(rating)
                        .ratedAt(Instant.now())
                        . build());
            }
        }

        return booking;
    }

    // ==================== DASHBOARD OVERVIEW TESTS ====================

    @Test
    void getDashboardOverview_ShouldReturnCompleteOverview() {
        // Arrange
        when(bookingRepository. findAll()).thenReturn(testBookings);

        // Act
        DashboardOverviewDTO overview = dashboardService.getDashboardOverview();

        // Assert
        assertThat(overview).isNotNull();
        assertThat(overview.getTotalBookings()).isEqualTo(5);
        assertThat(overview.getPendingBookings()).isEqualTo(1);
        assertThat(overview.getAssignedBookings()).isEqualTo(1);
        assertThat(overview.getCompletedBookings()).isEqualTo(2);
        assertThat(overview.getCancelledBookings()).isEqualTo(1);
        assertThat(overview.getBookingsByStatus()).isNotEmpty();
        assertThat(overview.getBookingsByCategory()).isNotEmpty();
        assertThat(overview.getAvgRating()).isGreaterThan(0);
    }

    @Test
    void getDashboardOverview_ShouldCalculateTodayStats() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(testBookings);

        // Act
        DashboardOverviewDTO overview = dashboardService. getDashboardOverview();

        // Assert
        assertThat(overview.getTodayBookings()).isEqualTo(2); // booking1 and booking2
        verify(bookingRepository, times(1)).findAll();
    }

    @Test
    void getDashboardOverview_ShouldHandleEmptyBookings() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        DashboardOverviewDTO overview = dashboardService.getDashboardOverview();

        // Assert
        assertThat(overview).isNotNull();
        assertThat(overview.getTotalBookings()).isZero();
        assertThat(overview.getAvgRating()).isZero();
        assertThat(overview.getBookingsByStatus()).isEmpty();
    }

    @Test
    void getDashboardOverview_ShouldCalculateAverageRating() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(testBookings);

        // Act
        DashboardOverviewDTO overview = dashboardService. getDashboardOverview();

        // Assert - (5 + 4) / 2 = 4.5
        assertThat(overview. getAvgRating()).isEqualTo(4.5);
    }

    // ==================== BOOKINGS BY STATUS TESTS ====================

    @Test
    void getBookingsByStatus_ShouldReturnStatusCounts() {
        // Arrange
        when(bookingRepository. findAll()).thenReturn(testBookings);

        // Act
        Map<String, Long> result = dashboardService.getBookingsByStatus();

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result.get("PENDING")).isEqualTo(1);
        assertThat(result.get("ASSIGNED")).isEqualTo(1);
        assertThat(result.get("COMPLETED")).isEqualTo(2);
        assertThat(result.get("CANCELLED")).isEqualTo(1);
    }

    @Test
    void getBookingsByStatus_ShouldHandleEmptyList() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(Collections. emptyList());

        // Act
        Map<String, Long> result = dashboardService.getBookingsByStatus();

        // Assert
        assertThat(result).isEmpty();
    }

    // ==================== BOOKINGS BY CATEGORY TESTS ====================

    @Test
    void getBookingsByCategory_ShouldReturnCategoryCounts() {
        // Arrange
        when(bookingRepository. findAll()).thenReturn(testBookings);

        // Act
        Map<String, Long> result = dashboardService.getBookingsByCategory();

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result.get("HVAC")).isEqualTo(2);
        assertThat(result. get("Plumbing")).isEqualTo(2);
        assertThat(result.get("Electrical")).isEqualTo(1);
    }

    @Test
    void getBookingsByCategory_ShouldIgnoreNullCategories() {
        // Arrange
        testBookings.add(createBooking("booking6", BookingStatus.PENDING, null, null, Instant.now()));
        when(bookingRepository.findAll()).thenReturn(testBookings);

        // Act
        Map<String, Long> result = dashboardService.getBookingsByCategory();

        // Assert
        assertThat(result).doesNotContainKey(null);
        assertThat(result. values().stream().mapToLong(Long::longValue).sum()).isEqualTo(5);
    }

    // ==================== BOOKINGS BY DATE RANGE TESTS ====================

    @Test
    void getBookingsByDateRange_ShouldReturnDateCounts() {
        // Arrange
        LocalDate startDate = today.minusDays(7);
        LocalDate endDate = today;
        when(bookingRepository. findAll()).thenReturn(testBookings);

        // Act
        Map<String, Long> result = dashboardService.getBookingsByDateRange(startDate, endDate);

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result.keySet()).allMatch(date -> {
            LocalDate parsedDate = LocalDate.parse(date);
            return ! parsedDate.isBefore(startDate) && !parsedDate.isAfter(endDate);
        });
    }

    @Test
    void getBookingsByDateRange_ShouldReturnSortedDates() {
        // Arrange
        LocalDate startDate = today.minusDays(7);
        LocalDate endDate = today;
        when(bookingRepository. findAll()).thenReturn(testBookings);

        // Act
        Map<String, Long> result = dashboardService.getBookingsByDateRange(startDate, endDate);

        // Assert - TreeMap should maintain sorted order
        assertThat(result).isInstanceOf(TreeMap.class);
        List<String> dates = new ArrayList<>(result.keySet());
        List<String> sortedDates = new ArrayList<>(dates);
        Collections.sort(sortedDates);
        assertThat(dates).isEqualTo(sortedDates);
    }

    @Test
    void getBookingsByDateRange_ShouldExcludeOutOfRangeBookings() {
        // Arrange
        LocalDate startDate = today.minusDays(3);
        LocalDate endDate = today;
        when(bookingRepository. findAll()).thenReturn(testBookings);

        // Act
        Map<String, Long> result = dashboardService.getBookingsByDateRange(startDate, endDate);

        // Assert - booking5 (5 days ago) should be excluded
        long totalInRange = result.values().stream().mapToLong(Long::longValue).sum();
        assertThat(totalInRange).isLessThan(testBookings.size());
    }
}