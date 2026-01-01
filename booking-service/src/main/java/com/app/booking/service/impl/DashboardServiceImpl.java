package com.app.booking.service. impl;

import com.app. booking.dto.response.*;
import com.app.booking. model. Booking;
import com.app.booking.model.BookingStatus;
import com.app.booking.repository.BookingRepository;
import com.app.booking.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;

    // ==================== DASHBOARD OVERVIEW ====================

    @Override
    public DashboardOverviewDTO getDashboardOverview() {
        List<Booking> allBookings = bookingRepository.findAll();

        // Today's date range
        LocalDate today = LocalDate.now();
        Instant todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant todayEnd = today.plusDays(1).atStartOfDay(ZoneId. systemDefault()).toInstant();

        // This week's date range
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        Instant weekStartInstant = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant();

        // This month's date range
        LocalDate monthStart = today.withDayOfMonth(1);
        Instant monthStartInstant = monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant();

        // Count by status using streams
        Map<String, Long> bookingsByStatus = allBookings.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getStatus().name(),
                        Collectors.counting()
                ));

        // Count by category using streams
        Map<String, Long> bookingsByCategory = allBookings.stream()
                .filter(b -> b.getCategoryName() != null)
                .collect(Collectors.groupingBy(
                        Booking::getCategoryName,
                        Collectors.counting()
                ));

        // Today's bookings
        long todayBookings = allBookings.stream()
                .filter(b -> b.getCreatedAt() != null &&
                        b.getCreatedAt().isAfter(todayStart) &&
                        b.getCreatedAt().isBefore(todayEnd))
                .count();

        long todayCompleted = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED &&
                        b.getCompletedAt() != null &&
                        b.getCompletedAt().isAfter(todayStart) &&
                        b. getCompletedAt().isBefore(todayEnd))
                .count();

        // This week's bookings
        long weekBookings = allBookings. stream()
                .filter(b -> b.getCreatedAt() != null &&
                        b. getCreatedAt().isAfter(weekStartInstant))
                .count();

        long weekCompleted = allBookings. stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED &&
                        b.getCompletedAt() != null &&
                        b.getCompletedAt().isAfter(weekStartInstant))
                .count();

        // This month's bookings
        long monthBookings = allBookings. stream()
                .filter(b -> b.getCreatedAt() != null &&
                        b. getCreatedAt().isAfter(monthStartInstant))
                .count();

        long monthCompleted = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED &&
                        b.getCompletedAt() != null &&
                        b.getCompletedAt().isAfter(monthStartInstant))
                .count();

        // Average rating
        double avgRating = allBookings.stream()
                .filter(b -> b.getRatingFeedback() != null && b.getRatingFeedback().getRating() != null)
                .mapToInt(b -> b.getRatingFeedback().getRating())
                .average()
                .orElse(0.0);

        // Average resolution time
        double avgResolutionTime = calculateAvgResolutionTime(allBookings);

        return DashboardOverviewDTO.builder()
                .totalBookings(allBookings.size())
                .pendingBookings(bookingsByStatus.getOrDefault(BookingStatus.PENDING. name(), 0L))
                .assignedBookings(bookingsByStatus.getOrDefault(BookingStatus.ASSIGNED.name(), 0L))
                .inProgressBookings(bookingsByStatus.getOrDefault(BookingStatus.IN_PROGRESS.name(), 0L))
                .completedBookings(bookingsByStatus.getOrDefault(BookingStatus.COMPLETED.name(), 0L))
                .cancelledBookings(bookingsByStatus.getOrDefault(BookingStatus.CANCELLED.name(), 0L))
                .todayBookings(todayBookings)
                .todayCompleted(todayCompleted)
                .weekBookings(weekBookings)
                .weekCompleted(weekCompleted)
                .monthBookings(monthBookings)
                .monthCompleted(monthCompleted)
                .avgRating(Math.round(avgRating * 100.0) / 100.0)
                .avgResolutionTimeHours(Math.round(avgResolutionTime * 100.0) / 100.0)
                .bookingsByStatus(bookingsByStatus)
                .bookingsByCategory(bookingsByCategory)
                .build();
    }

    // ==================== BOOKING REPORTS ====================

    @Override
    public Map<String, Long> getBookingsByStatus() {
        return bookingRepository. findAll().stream()
                .collect(Collectors.groupingBy(
                        b -> b.getStatus().name(),
                        Collectors.counting()
                ));
    }

    @Override
    public Map<String, Long> getBookingsByCategory() {
        return bookingRepository.findAll().stream()
                .filter(b -> b. getCategoryName() != null)
                .collect(Collectors.groupingBy(
                        Booking::getCategoryName,
                        Collectors.counting()
                ));
    }

    @Override
    public Map<String, Long> getBookingsByDateRange(LocalDate startDate, LocalDate endDate) {
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        return bookingRepository.findAll().stream()
                .filter(b -> b.getCreatedAt() != null &&
                        b.getCreatedAt().isAfter(startInstant) &&
                        b.getCreatedAt().isBefore(endInstant))
                .collect(Collectors.groupingBy(
                        b -> b.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                        TreeMap::new,
                        Collectors.counting()
                ));
    }

    // ==================== TECHNICIAN WORKLOAD REPORT ====================

    @Override
    public List<TechnicianWorkloadDTO> getTechnicianWorkloadReport() {
        List<Booking> allBookings = bookingRepository.findAll();

        // Group by technician
        Map<String, List<Booking>> bookingsByTechnician = allBookings.stream()
                .filter(b -> b.getTechnicianId() != null)
                .collect(Collectors.groupingBy(Booking::getTechnicianId));

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        Instant monthStartInstant = monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<TechnicianWorkloadDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<Booking>> entry : bookingsByTechnician.entrySet()) {
            String techId = entry.getKey();
            List<Booking> techBookings = entry.getValue();

            long assigned = techBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.ASSIGNED)
                    . count();

            long inProgress = techBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.IN_PROGRESS)
                    .count();

            long completed = techBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                    .count();

            long completedThisMonth = techBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED &&
                            b.getCompletedAt() != null &&
                            b.getCompletedAt().isAfter(monthStartInstant))
                    .count();

            long totalActive = assigned + inProgress;
            String workloadStatus = calculateWorkloadStatus(totalActive);

            // Get technician name from first booking
            String techName = techBookings.stream()
                    .filter(b -> b.getTechnicianName() != null)
                    . map(Booking::getTechnicianName)
                    .findFirst()
                    .orElse("Unknown");

            TechnicianWorkloadDTO dto = TechnicianWorkloadDTO. builder()
                    .technicianId(techId)
                    .technicianName(techName)
                    .assignedBookings(assigned)
                    .inProgressBookings(inProgress)
                    .totalActiveBookings(totalActive)
                    .completedBookings(completed)
                    .completedThisMonth(completedThisMonth)
                    .workloadStatus(workloadStatus)
                    .build();

            result.add(dto);
        }

        // Sort by total active bookings (descending)
        result.sort((a, b) -> Long.compare(b.getTotalActiveBookings(), a.getTotalActiveBookings()));

        return result;
    }

    // ==================== TECHNICIAN PERFORMANCE REPORT ====================

    @Override
    public List<TechnicianPerformanceDTO> getTechnicianPerformanceReport() {
        List<Booking> allBookings = bookingRepository.findAll();

        // Group by technician
        Map<String, List<Booking>> bookingsByTechnician = allBookings.stream()
                .filter(b -> b.getTechnicianId() != null)
                .collect(Collectors.groupingBy(Booking::getTechnicianId));

        List<TechnicianPerformanceDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<Booking>> entry :  bookingsByTechnician.entrySet()) {
            String techId = entry.getKey();
            List<Booking> techBookings = entry.getValue();

            // Completed jobs
            long completed = techBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                    .count();

            // Rejected jobs
            long rejected = techBookings.stream()
                    . filter(b -> b.getStatus() == BookingStatus.REJECTED)
                    .count();

            long totalAssigned = techBookings.size();
            double rejectionRate = totalAssigned > 0 ? (rejected * 100.0 / totalAssigned) : 0;

            // Average rating
            List<Integer> ratings = techBookings.stream()
                    .filter(b -> b.getRatingFeedback() != null && b.getRatingFeedback().getRating() != null)
                    . map(b -> b.getRatingFeedback().getRating())
                    .collect(Collectors.toList());

            double avgRating = ratings.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);

            // Average resolution time
            double avgTime = techBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED &&
                            b.getCreatedAt() != null && b.getCompletedAt() != null)
                    .mapToDouble(b -> ChronoUnit.HOURS.between(
                            b.getCreatedAt().atZone(ZoneId.systemDefault()),
                            b.getCompletedAt().atZone(ZoneId.systemDefault())))
                    .average()
                    .orElse(0.0);

            // Get technician name
            String techName = techBookings.stream()
                    .filter(b -> b.getTechnicianName() != null)
                    .map(Booking::getTechnicianName)
                    .findFirst()
                    . orElse("Unknown");

            TechnicianPerformanceDTO dto = TechnicianPerformanceDTO.builder()
                    .technicianId(techId)
                    .technicianName(techName)
                    .totalJobsCompleted(completed)
                    . avgRating(Math.round(avgRating * 100.0) / 100.0)
                    .totalRatings(ratings.size())
                    .avgResolutionTimeHours(Math.round(avgTime * 100.0) / 100.0)
                    .totalAssigned(totalAssigned)
                    .totalRejected(rejected)
                    .rejectionRate(Math.round(rejectionRate * 100.0) / 100.0)
                    .onTimeCompletions(completed)
                    .lateCompletions(0)
                    .onTimeRate(100.0)
                    .build();

            result.add(dto);
        }

        // Sort by average rating (descending)
        result.sort((a, b) -> Double.compare(b.getAvgRating(), a.getAvgRating()));

        return result;
    }

    // ==================== RESOLUTION TIME REPORT ====================

    @Override
    public ResolutionTimeReportDTO getResolutionTimeReport() {
        List<Booking> completedBookings = bookingRepository. findByStatus(BookingStatus.COMPLETED);

        // Calculate resolution times
        List<Double> resolutionTimes = completedBookings.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCompletedAt() != null)
                .map(b -> (double) ChronoUnit.HOURS.between(
                        b.getCreatedAt().atZone(ZoneId.systemDefault()),
                        b.getCompletedAt().atZone(ZoneId.systemDefault())))
                .filter(hours -> hours >= 0)
                .collect(Collectors.toList());

        double avgResolutionTime = resolutionTimes.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double minTime = resolutionTimes.stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);

        double maxTime = resolutionTimes.stream()
                .mapToDouble(Double:: doubleValue)
                .max()
                .orElse(0.0);

        // Average by category
        Map<String, Double> avgTimeByCategory = completedBookings.stream()
                .filter(b -> b.getCategoryName() != null &&
                        b.getCreatedAt() != null &&
                        b.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                        Booking::getCategoryName,
                        Collectors.averagingDouble(b -> ChronoUnit.HOURS.between(
                                b. getCreatedAt().atZone(ZoneId.systemDefault()),
                                b.getCompletedAt().atZone(ZoneId.systemDefault())))
                ));

        // Round the values
        Map<String, Double> roundedAvgTimeByCategory = new HashMap<>();
        for (Map.Entry<String, Double> entry :  avgTimeByCategory.entrySet()) {
            roundedAvgTimeByCategory.put(entry.getKey(), Math.round(entry.getValue() * 100.0) / 100.0);
        }

        // Average by priority
        Map<String, Double> avgTimeByPriority = completedBookings.stream()
                .filter(b -> b.getPriority() != null &&
                        b.getCreatedAt() != null &&
                        b. getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getPriority().name(),
                        Collectors.averagingDouble(b -> ChronoUnit.HOURS.between(
                                b.getCreatedAt().atZone(ZoneId.systemDefault()),
                                b.getCompletedAt().atZone(ZoneId. systemDefault())))
                ));

        // Round the values
        Map<String, Double> roundedAvgTimeByPriority = new HashMap<>();
        for (Map.Entry<String, Double> entry : avgTimeByPriority. entrySet()) {
            roundedAvgTimeByPriority.put(entry.getKey(), Math.round(entry.getValue() * 100.0) / 100.0);
        }

        return ResolutionTimeReportDTO. builder()
                .avgResolutionTimeHours(Math.round(avgResolutionTime * 100.0) / 100.0)
                .avgAssignmentTimeHours(0.0)
                .avgServiceTimeHours(0.0)
                .minResolutionTimeHours(Math.round(minTime * 100.0) / 100.0)
                .maxResolutionTimeHours(Math.round(maxTime * 100.0) / 100.0)
                .avgTimeByCategory(roundedAvgTimeByCategory)
                .avgTimeByPriority(roundedAvgTimeByPriority)
                .totalCompletedBookings(completedBookings.size())
                .build();
    }

    // ==================== CUSTOMER SATISFACTION REPORT ====================

    @Override
    public CustomerSatisfactionDTO getCustomerSatisfactionReport() {
        List<Booking> ratedBookings = bookingRepository. findAll().stream()
                .filter(b -> b.getRatingFeedback() != null && b.getRatingFeedback().getRating() != null)
                .collect(Collectors.toList());

        if (ratedBookings.isEmpty()) {
            return CustomerSatisfactionDTO.builder()
                    . avgRating(0.0)
                    .totalRatings(0)
                    .fiveStarCount(0)
                    .fourStarCount(0)
                    .threeStarCount(0)
                    .twoStarCount(0)
                    .oneStarCount(0)
                    .fiveStarPercentage(0.0)
                    .fourStarPercentage(0.0)
                    .threeStarPercentage(0.0)
                    .twoStarPercentage(0.0)
                    .oneStarPercentage(0.0)
                    .avgRatingByCategory(new HashMap<>())
                    .satisfactionRate(0.0)
                    .build();
        }

        // Rating distribution
        Map<Integer, Long> ratingCounts = ratedBookings.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getRatingFeedback().getRating(),
                        Collectors.counting()
                ));

        long totalRatings = ratedBookings.size();
        long fiveStar = ratingCounts. getOrDefault(5, 0L);
        long fourStar = ratingCounts.getOrDefault(4, 0L);
        long threeStar = ratingCounts.getOrDefault(3, 0L);
        long twoStar = ratingCounts.getOrDefault(2, 0L);
        long oneStar = ratingCounts.getOrDefault(1, 0L);

        double avgRating = ratedBookings.stream()
                .mapToInt(b -> b.getRatingFeedback().getRating())
                .average()
                .orElse(0.0);

        // Rating by category
        Map<String, Double> avgRatingByCategory = ratedBookings.stream()
                .filter(b -> b.getCategoryName() != null)
                .collect(Collectors. groupingBy(
                        Booking::getCategoryName,
                        Collectors.averagingDouble(b -> b.getRatingFeedback().getRating())
                ));

        // Round the values
        Map<String, Double> roundedAvgRatingByCategory = new HashMap<>();
        for (Map.Entry<String, Double> entry : avgRatingByCategory.entrySet()) {
            roundedAvgRatingByCategory.put(entry.getKey(), Math.round(entry.getValue() * 100.0) / 100.0);
        }

        // Satisfaction rate (4+ stars)
        long satisfiedCustomers = fiveStar + fourStar;
        double satisfactionRate = totalRatings > 0 ?  (satisfiedCustomers * 100.0 / totalRatings) : 0;

        return CustomerSatisfactionDTO.builder()
                .avgRating(Math.round(avgRating * 100.0) / 100.0)
                .totalRatings(totalRatings)
                .fiveStarCount(fiveStar)
                .fourStarCount(fourStar)
                .threeStarCount(threeStar)
                .twoStarCount(twoStar)
                .oneStarCount(oneStar)
                .fiveStarPercentage(totalRatings > 0 ?  Math.round(fiveStar * 10000.0 / totalRatings) / 100.0 : 0)
                .fourStarPercentage(totalRatings > 0 ? Math.round(fourStar * 10000.0 / totalRatings) / 100.0 : 0)
                .threeStarPercentage(totalRatings > 0 ? Math.round(threeStar * 10000.0 / totalRatings) / 100.0 : 0)
                .twoStarPercentage(totalRatings > 0 ? Math.round(twoStar * 10000.0 / totalRatings) / 100.0 : 0)
                .oneStarPercentage(totalRatings > 0 ? Math.round(oneStar * 10000.0 / totalRatings) / 100.0 : 0)
                .avgRatingByCategory(roundedAvgRatingByCategory)
                .satisfactionRate(Math.round(satisfactionRate * 100.0) / 100.0)
                .build();
    }

    // ==================== MONTHLY SUMMARY REPORT ====================

    @Override
    public MonthlySummaryDTO getMonthlySummary(int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart. plusMonths(1);
        LocalDate prevMonthStart = monthStart.minusMonths(1);

        Instant monthStartInstant = monthStart.atStartOfDay(ZoneId. systemDefault()).toInstant();
        Instant monthEndInstant = monthEnd.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant prevMonthStartInstant = prevMonthStart. atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Booking> allBookings = bookingRepository.findAll();

        // This month's bookings
        List<Booking> monthBookings = allBookings.stream()
                .filter(b -> b.getCreatedAt() != null &&
                        b.getCreatedAt().isAfter(monthStartInstant) &&
                        b.getCreatedAt().isBefore(monthEndInstant))
                .collect(Collectors.toList());

        // Previous month's bookings count
        long prevMonthCount = allBookings.stream()
                .filter(b -> b. getCreatedAt() != null &&
                        b.getCreatedAt().isAfter(prevMonthStartInstant) &&
                        b. getCreatedAt().isBefore(monthStartInstant))
                .count();

        // Stats
        long totalBookings = monthBookings.size();

        long completedBookings = monthBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .count();

        long cancelledBookings = monthBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                .count();

        double completionRate = totalBookings > 0 ? (completedBookings * 100.0 / totalBookings) : 0;
        double growthRate = prevMonthCount > 0 ?  ((totalBookings - prevMonthCount) * 100.0 / prevMonthCount) : 0;

        // By category
        Map<String, Long> byCategory = monthBookings.stream()
                .filter(b -> b.getCategoryName() != null)
                .collect(Collectors. groupingBy(Booking::getCategoryName, Collectors.counting()));

        // By status
        Map<String, Long> byStatus = monthBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getStatus().name(), Collectors.counting()));

        // Average rating
        double avgRating = monthBookings.stream()
                .filter(b -> b.getRatingFeedback() != null && b.getRatingFeedback().getRating() != null)
                .mapToInt(b -> b.getRatingFeedback().getRating())
                .average()
                .orElse(0.0);

        // Average resolution time
        double avgResolutionTime = monthBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED &&
                        b.getCreatedAt() != null && b.getCompletedAt() != null)
                .mapToDouble(b -> ChronoUnit.HOURS.between(
                        b.getCreatedAt().atZone(ZoneId.systemDefault()),
                        b.getCompletedAt().atZone(ZoneId. systemDefault())))
                .average()
                .orElse(0.0);

        // Top technician
        Map<String, Long> technicianCompletions = monthBookings.stream()
                .filter(b -> b. getStatus() == BookingStatus.COMPLETED && b.getTechnicianId() != null)
                .collect(Collectors.groupingBy(Booking::getTechnicianId, Collectors.counting()));

        String topTechId = null;
        String topTechName = null;
        long topTechCompletions = 0;

        if (!technicianCompletions. isEmpty()) {
            // Find the technician with most completions
            Optional<Map.Entry<String, Long>> topEntryOpt = technicianCompletions.entrySet().stream()
                    .max(Map.Entry.comparingByValue());

            if (topEntryOpt.isPresent()) {
                Map.Entry<String, Long> topEntry = topEntryOpt. get();
                topTechId = topEntry.getKey();
                topTechCompletions = topEntry.getValue();

                // Need final variable for lambda
                final String finalTopTechId = topTechId;
                topTechName = monthBookings.stream()
                        .filter(b -> finalTopTechId.equals(b.getTechnicianId()) && b.getTechnicianName() != null)
                        .map(Booking::getTechnicianName)
                        .findFirst()
                        .orElse("Unknown");
            }
        }

        return MonthlySummaryDTO. builder()
                .year(year)
                .month(month)
                .monthName(monthStart.getMonth().getDisplayName(TextStyle.FULL, Locale. ENGLISH))
                .totalBookings(totalBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .completionRate(Math.round(completionRate * 100.0) / 100.0)
                .previousMonthBookings(prevMonthCount)
                .bookingsGrowthRate(Math.round(growthRate * 100.0) / 100.0)
                .bookingsByCategory(byCategory)
                .bookingsByStatus(byStatus)
                .avgRating(Math.round(avgRating * 100.0) / 100.0)
                .avgResolutionTimeHours(Math.round(avgResolutionTime * 100.0) / 100.0)
                .topTechnicianId(topTechId)
                .topTechnicianName(topTechName)
                .topTechnicianCompletions(topTechCompletions)
                .build();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Calculate average resolution time for completed bookings
     */
    private double calculateAvgResolutionTime(List<Booking> bookings) {
        return bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED &&
                        b.getCreatedAt() != null && b.getCompletedAt() != null)
                .mapToDouble(b -> ChronoUnit.HOURS.between(
                        b. getCreatedAt().atZone(ZoneId.systemDefault()),
                        b.getCompletedAt().atZone(ZoneId.systemDefault())))
                .average()
                .orElse(0.0);
    }

    /**
     * Calculate workload status based on active jobs count
     */
    private String calculateWorkloadStatus(long activeJobs) {
        if (activeJobs == 0) {
            return "IDLE";
        } else if (activeJobs <= 2) {
            return "LOW";
        } else if (activeJobs <= 4) {
            return "MEDIUM";
        } else if (activeJobs <= 6) {
            return "HIGH";
        } else {
            return "OVERLOADED";
        }
    }
}