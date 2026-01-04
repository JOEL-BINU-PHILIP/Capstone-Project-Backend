package com.app.booking.service.impl;

import com.app.booking.dto.response.TechnicianPerformanceDTO;
import com.app.booking.dto.response.TechnicianWorkloadDTO;
import com.app. booking.model.Booking;
import com.app.booking.model.BookingStatus;
import com.app.booking.model.RatingFeedback;
import com.app.booking.repository.BookingRepository;
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
class DashboardServiceImplTest_TechnicianReports {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private List<Booking> technicianBookings;
    private Instant monthStart;

    @BeforeEach
    void setUp() {
        monthStart = LocalDate.now().withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant();

        technicianBookings = new ArrayList<>();

        // Tech1 bookings
        technicianBookings.add(createTechBooking("b1", "tech1", "John Doe", BookingStatus.ASSIGNED, null));
        technicianBookings. add(createTechBooking("b2", "tech1", "John Doe", BookingStatus.IN_PROGRESS, null));
        technicianBookings.add(createTechBooking("b3", "tech1", "John Doe", BookingStatus.COMPLETED, 5));
        technicianBookings.add(createTechBooking("b4", "tech1", "John Doe", BookingStatus.COMPLETED, 4));

        // Tech2 bookings
        technicianBookings.add(createTechBooking("b5", "tech2", "Jane Smith", BookingStatus.COMPLETED, 5));
        technicianBookings.add(createTechBooking("b6", "tech2", "Jane Smith", BookingStatus. REJECTED, null));

        // Tech3 bookings
        technicianBookings.add(createTechBooking("b7", "tech3", "Bob Johnson", BookingStatus.ASSIGNED, null));
        technicianBookings.add(createTechBooking("b8", "tech3", "Bob Johnson", BookingStatus.ASSIGNED, null));
        technicianBookings.add(createTechBooking("b9", "tech3", "Bob Johnson", BookingStatus.ASSIGNED, null));
    }

    private Booking createTechBooking(String id, String techId, String techName,
                                      BookingStatus status, Integer rating) {
        Booking booking = Booking.builder()
                .id(id)
                .technicianId(techId)
                .technicianName(techName)
                .status(status)
                .createdAt(monthStart)
                .build();

        if (status == BookingStatus.COMPLETED) {
            booking.setCompletedAt(monthStart.plus(Duration.ofHours(24)));
            if (rating != null) {
                booking.setRatingFeedback(RatingFeedback.builder()
                        .rating(rating)
                        .ratedAt(Instant. now())
                        .build());
            }
        }

        return booking;
    }

    // ==================== TECHNICIAN WORKLOAD TESTS ====================

    @Test
    void getTechnicianWorkloadReport_ShouldReturnWorkloadForAllTechnicians() {
        // Arrange
        when(bookingRepository. findAll()).thenReturn(technicianBookings);

        // Act
        List<TechnicianWorkloadDTO> result = dashboardService.getTechnicianWorkloadReport();

        // Assert
        assertThat(result).hasSize(3); // 3 technicians
        assertThat(result).extracting(TechnicianWorkloadDTO::getTechnicianId)
                .containsExactlyInAnyOrder("tech1", "tech2", "tech3");
    }

    @Test
    void getTechnicianWorkloadReport_ShouldCalculateCorrectCounts() {
        // Arrange
        when(bookingRepository. findAll()).thenReturn(technicianBookings);

        // Act
        List<TechnicianWorkloadDTO> result = dashboardService.getTechnicianWorkloadReport();

        // Assert
        TechnicianWorkloadDTO tech1 = result.stream()
                .filter(t -> t.getTechnicianId().equals("tech1"))
                .findFirst()
                .orElseThrow();

        assertThat(tech1.getAssignedBookings()).isEqualTo(1);
        assertThat(tech1.getInProgressBookings()).isEqualTo(1);
        assertThat(tech1.getTotalActiveBookings()).isEqualTo(2);
        assertThat(tech1.getCompletedBookings()).isEqualTo(2);
    }

    @Test
    void getTechnicianWorkloadReport_ShouldCalculateWorkloadStatus() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(technicianBookings);

        // Act
        List<TechnicianWorkloadDTO> result = dashboardService.getTechnicianWorkloadReport();

        // Assert
        TechnicianWorkloadDTO tech3 = result.stream()
                .filter(t -> t.getTechnicianId().equals("tech3"))
                .findFirst()
                .orElseThrow();

        // tech3 has 3 assigned bookings (MEDIUM workload:  3-4 jobs)
        assertThat(tech3.getTotalActiveBookings()).isEqualTo(3);
        assertThat(tech3.getWorkloadStatus()).isEqualTo("MEDIUM");
    }

    @Test
    void getTechnicianWorkloadReport_ShouldSortByActiveBookings() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(technicianBookings);

        // Act
        List<TechnicianWorkloadDTO> result = dashboardService.getTechnicianWorkloadReport();

        // Assert - Should be sorted descending by total active bookings
        assertThat(result. get(0).getTechnicianId()).isEqualTo("tech3"); // 3 active
        assertThat(result.get(1).getTechnicianId()).isEqualTo("tech1"); // 2 active
        assertThat(result.get(2).getTechnicianId()).isEqualTo("tech2"); // 0 active
    }

    @Test
    void getTechnicianWorkloadReport_ShouldHandleNoBookings() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<TechnicianWorkloadDTO> result = dashboardService.getTechnicianWorkloadReport();

        // Assert
        assertThat(result).isEmpty();
    }

    // ==================== TECHNICIAN PERFORMANCE TESTS ====================

    @Test
    void getTechnicianPerformanceReport_ShouldReturnPerformanceForAllTechnicians() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(technicianBookings);

        // Act
        List<TechnicianPerformanceDTO> result = dashboardService.getTechnicianPerformanceReport();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).extracting(TechnicianPerformanceDTO::getTechnicianId)
                .containsExactlyInAnyOrder("tech1", "tech2", "tech3");
    }

    @Test
    void getTechnicianPerformanceReport_ShouldCalculateAverageRating() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(technicianBookings);

        // Act
        List<TechnicianPerformanceDTO> result = dashboardService.getTechnicianPerformanceReport();

        // Assert
        TechnicianPerformanceDTO tech1 = result.stream()
                .filter(t -> t.getTechnicianId().equals("tech1"))
                .findFirst()
                .orElseThrow();

        // tech1 has ratings: 5 and 4, average = 4.5
        assertThat(tech1.getAvgRating()).isEqualTo(4.5);
        assertThat(tech1.getTotalRatings()).isEqualTo(2);
    }

    @Test
    void getTechnicianPerformanceReport_ShouldCalculateRejectionRate() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(technicianBookings);

        // Act
        List<TechnicianPerformanceDTO> result = dashboardService.getTechnicianPerformanceReport();

        // Assert
        TechnicianPerformanceDTO tech2 = result.stream()
                .filter(t -> t.getTechnicianId().equals("tech2"))
                .findFirst()
                .orElseThrow();

        // tech2: 2 total assigned, 1 rejected = 50% rejection rate
        assertThat(tech2.getTotalAssigned()).isEqualTo(2);
        assertThat(tech2.getTotalRejected()).isEqualTo(1);
        assertThat(tech2.getRejectionRate()).isEqualTo(50.0);
    }

    @Test
    void getTechnicianPerformanceReport_ShouldSortByRating() {
        // Arrange
        when(bookingRepository. findAll()).thenReturn(technicianBookings);

        // Act
        List<TechnicianPerformanceDTO> result = dashboardService.getTechnicianPerformanceReport();

        // Assert - Should be sorted descending by average rating
        // tech2: 5. 0, tech1: 4.5, tech3: 0.0
        assertThat(result.get(0).getAvgRating()).isGreaterThanOrEqualTo(result.get(1).getAvgRating());
        assertThat(result.get(1).getAvgRating()).isGreaterThanOrEqualTo(result.get(2).getAvgRating());
    }

    @Test
    void getTechnicianPerformanceReport_ShouldHandleZeroRatings() {
        // Arrange
        when(bookingRepository. findAll()).thenReturn(technicianBookings);

        // Act
        List<TechnicianPerformanceDTO> result = dashboardService.getTechnicianPerformanceReport();

        // Assert
        TechnicianPerformanceDTO tech3 = result.stream()
                .filter(t -> t. getTechnicianId().equals("tech3"))
                .findFirst()
                .orElseThrow();

        // tech3 has no completed/rated bookings
        assertThat(tech3.getAvgRating()).isZero();
        assertThat(tech3.getTotalRatings()).isZero();
    }

    @Test
    void getTechnicianPerformanceReport_ShouldCalculateCompletedJobs() {
        // Arrange
        when(bookingRepository. findAll()).thenReturn(technicianBookings);

        // Act
        List<TechnicianPerformanceDTO> result = dashboardService.getTechnicianPerformanceReport();

        // Assert
        TechnicianPerformanceDTO tech1 = result.stream()
                .filter(t -> t.getTechnicianId().equals("tech1"))
                .findFirst()
                .orElseThrow();

        assertThat(tech1.getTotalJobsCompleted()).isEqualTo(2);
    }
}