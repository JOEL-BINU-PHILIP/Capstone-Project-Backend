package com.app. booking.service.impl;

import com.app.booking.dto. response.*;
import com.app.booking. model.*;
import com.app. booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org. junit.jupiter.api.Test;
import org.junit.jupiter. api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org. mockito.Mock;
import org.mockito.junit.jupiter. MockitoExtension;

import java.time.*;
import java. util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest_OtherReports {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private List<Booking> completedBookings;

    @BeforeEach
    void setUp() {
        completedBookings = new ArrayList<>();

        Instant baseTime = Instant.now().minus(Duration.ofDays(5));

        // Booking completed in 24 hours
        Booking b1 = createCompletedBooking("b1", baseTime, baseTime.plus(Duration.ofHours(24)),
                "HVAC", Priority. NORMAL, 5);
        completedBookings.add(b1);

        // Booking completed in 12 hours
        Booking b2 = createCompletedBooking("b2", baseTime, baseTime.plus(Duration.ofHours(12)),
                "Plumbing", Priority.HIGH, 4);
        completedBookings. add(b2);

        // Booking completed in 48 hours
        Booking b3 = createCompletedBooking("b3", baseTime, baseTime.plus(Duration.ofHours(48)),
                "Electrical", Priority.LOW, 3);
        completedBookings.add(b3);
    }

    private Booking createCompletedBooking(String id, Instant created, Instant completed,
                                           String category, Priority priority, Integer rating) {
        Booking booking = Booking.builder()
                .id(id)
                .status(BookingStatus.COMPLETED)
                .categoryName(category)
                .priority(priority)
                .createdAt(created)
                .completedAt(completed)
                .build();

        if (rating != null) {
            booking. setRatingFeedback(RatingFeedback.builder()
                    .rating(rating)
                    .ratedAt(Instant.now())
                    . build());
        }

        return booking;
    }

    // ==================== RESOLUTION TIME REPORT TESTS ====================

    @Test
    void getResolutionTimeReport_ShouldCalculateAverageTime() {
        // Arrange
        when(bookingRepository.findByStatus(BookingStatus.COMPLETED)).thenReturn(completedBookings);

        // Act
        ResolutionTimeReportDTO result = dashboardService.getResolutionTimeReport();

        // Assert
        // Average of 24, 12, 48 = 28 hours
        assertThat(result. getAvgResolutionTimeHours()).isEqualTo(28.0);
        assertThat(result.getTotalCompletedBookings()).isEqualTo(3);
    }

    @Test
    void getResolutionTimeReport_ShouldCalculateMinMaxTime() {
        // Arrange
        when(bookingRepository. findByStatus(BookingStatus. COMPLETED)).thenReturn(completedBookings);

        // Act
        ResolutionTimeReportDTO result = dashboardService.getResolutionTimeReport();

        // Assert
        assertThat(result.getMinResolutionTimeHours()).isEqualTo(12.0);
        assertThat(result.getMaxResolutionTimeHours()).isEqualTo(48.0);
    }

    @Test
    void getResolutionTimeReport_ShouldCalculateAverageByCategory() {
        // Arrange
        when(bookingRepository. findByStatus(BookingStatus. COMPLETED)).thenReturn(completedBookings);

        // Act
        ResolutionTimeReportDTO result = dashboardService.getResolutionTimeReport();

        // Assert
        assertThat(result.getAvgTimeByCategory()).isNotEmpty();
        assertThat(result.getAvgTimeByCategory().get("HVAC")).isEqualTo(24.0);
        assertThat(result.getAvgTimeByCategory().get("Plumbing")).isEqualTo(12.0);
        assertThat(result.getAvgTimeByCategory().get("Electrical")).isEqualTo(48.0);
    }

    @Test
    void getResolutionTimeReport_ShouldCalculateAverageByPriority() {
        // Arrange
        when(bookingRepository.findByStatus(BookingStatus.COMPLETED)).thenReturn(completedBookings);

        // Act
        ResolutionTimeReportDTO result = dashboardService.getResolutionTimeReport();

        // Assert
        assertThat(result.getAvgTimeByPriority()).isNotEmpty();
        assertThat(result.getAvgTimeByPriority().get("NORMAL")).isEqualTo(24.0);
        assertThat(result.getAvgTimeByPriority().get("HIGH")).isEqualTo(12.0);
        assertThat(result.getAvgTimeByPriority().get("LOW")).isEqualTo(48.0);
    }

    @Test
    void getResolutionTimeReport_ShouldHandleNoCompletedBookings() {
        // Arrange
        when(bookingRepository.findByStatus(BookingStatus.COMPLETED))
                .thenReturn(Collections.emptyList());

        // Act
        ResolutionTimeReportDTO result = dashboardService. getResolutionTimeReport();

        // Assert
        assertThat(result. getAvgResolutionTimeHours()).isZero();
        assertThat(result.getTotalCompletedBookings()).isZero();
        assertThat(result.getAvgTimeByCategory()).isEmpty();
    }

    // ==================== CUSTOMER SATISFACTION REPORT TESTS ====================

    @Test
    void getCustomerSatisfactionReport_ShouldCalculateAverageRating() {
        // Arrange
        when(bookingRepository. findAll()).thenReturn(completedBookings);

        // Act
        CustomerSatisfactionDTO result = dashboardService.getCustomerSatisfactionReport();

        // Assert
        // Average of 5, 4, 3 = 4.0
        assertThat(result. getAvgRating()).isEqualTo(4.0);
        assertThat(result.getTotalRatings()).isEqualTo(3);
    }

    @Test
    void getCustomerSatisfactionReport_ShouldCalculateRatingDistribution() {
        // Arrange
        when(bookingRepository. findAll()).thenReturn(completedBookings);

        // Act
        CustomerSatisfactionDTO result = dashboardService.getCustomerSatisfactionReport();

        // Assert
        assertThat(result.getFiveStarCount()).isEqualTo(1);
        assertThat(result.getFourStarCount()).isEqualTo(1);
        assertThat(result.getThreeStarCount()).isEqualTo(1);
        assertThat(result.getTwoStarCount()).isZero();
        assertThat(result.getOneStarCount()).isZero();
    }

    @Test
    void getCustomerSatisfactionReport_ShouldCalculatePercentages() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(completedBookings);

        // Act
        CustomerSatisfactionDTO result = dashboardService.getCustomerSatisfactionReport();

        // Assert
        assertThat(result. getFiveStarPercentage()).isEqualTo(33.33);
        assertThat(result.getFourStarPercentage()).isEqualTo(33.33);
        assertThat(result.getThreeStarPercentage()).isEqualTo(33.33);
    }

    @Test
    void getCustomerSatisfactionReport_ShouldCalculateSatisfactionRate() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(completedBookings);

        // Act
        CustomerSatisfactionDTO result = dashboardService.getCustomerSatisfactionReport();

        // Assert
        // 2 ratings >= 4 out of 3 total = 66.67%
        assertThat(result.getSatisfactionRate()).isEqualTo(66.67);
    }

    @Test
    void getCustomerSatisfactionReport_ShouldCalculateAverageByCategory() {
        // Arrange
        when(bookingRepository. findAll()).thenReturn(completedBookings);

        // Act
        CustomerSatisfactionDTO result = dashboardService.getCustomerSatisfactionReport();

        // Assert
        assertThat(result.getAvgRatingByCategory()).isNotEmpty();
        assertThat(result. getAvgRatingByCategory().get("HVAC")).isEqualTo(5.0);
        assertThat(result.getAvgRatingByCategory().get("Plumbing")).isEqualTo(4.0);
        assertThat(result.getAvgRatingByCategory().get("Electrical")).isEqualTo(3.0);
    }

    @Test
    void getCustomerSatisfactionReport_ShouldHandleNoRatings() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        CustomerSatisfactionDTO result = dashboardService.getCustomerSatisfactionReport();

        // Assert
        assertThat(result.getAvgRating()).isZero();
        assertThat(result.getTotalRatings()).isZero();
        assertThat(result.getSatisfactionRate()).isZero();
        assertThat(result.getAvgRatingByCategory()).isEmpty();
    }

    // ==================== MONTHLY SUMMARY TESTS ====================

    @Test
    void getMonthlySummary_ShouldCalculateMonthlyStats() {
        // Arrange
        int year = 2026;
        int month = 1;
        when(bookingRepository.findAll()).thenReturn(completedBookings);

        // Act
        MonthlySummaryDTO result = dashboardService.getMonthlySummary(year, month);

        // Assert
        assertThat(result.getYear()).isEqualTo(year);
        assertThat(result. getMonth()).isEqualTo(month);
        assertThat(result.getMonthName()).isEqualTo("January");
    }

    @Test
    void getMonthlySummary_ShouldCalculateCompletionRate() {
        // Arrange
        // Add more bookings for the month
        Instant monthStart = LocalDate.of(2026, 1, 1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Booking> monthBookings = new ArrayList<>();
        monthBookings.add(createMonthBooking("m1", BookingStatus.COMPLETED, monthStart));
        monthBookings.add(createMonthBooking("m2", BookingStatus. COMPLETED, monthStart));
        monthBookings.add(createMonthBooking("m3", BookingStatus.PENDING, monthStart));
        monthBookings.add(createMonthBooking("m4", BookingStatus.CANCELLED, monthStart));

        when(bookingRepository.findAll()).thenReturn(monthBookings);

        // Act
        MonthlySummaryDTO result = dashboardService.getMonthlySummary(2026, 1);

        // Assert
        // 2 completed out of 4 total = 50%
        assertThat(result. getTotalBookings()).isEqualTo(4);
        assertThat(result.getCompletedBookings()).isEqualTo(2);
        assertThat(result.getCompletionRate()).isEqualTo(50.0);
    }

    private Booking createMonthBooking(String id, BookingStatus status, Instant createdAt) {
        return Booking.builder()
                .id(id)
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    @Test
    void getMonthlySummary_ShouldHandleEmptyMonth() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(Collections. emptyList());

        // Act
        MonthlySummaryDTO result = dashboardService.getMonthlySummary(2026, 1);

        // Assert
        assertThat(result. getTotalBookings()).isZero();
        assertThat(result.getCompletedBookings()).isZero();
        assertThat(result.getCompletionRate()).isZero();
    }
}