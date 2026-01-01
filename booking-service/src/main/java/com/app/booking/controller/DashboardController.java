package com.app.booking.controller;

import com.app.booking.dto.response.*;
import com.app.booking. service.DashboardService;
import lombok. RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org. springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework. security.access.prepost.PreAuthorize;
import org. springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Dashboard & Reports Controller
 * Provides statistics and reports for managers and admins
 */
@Slf4j
@RestController
@RequestMapping("/api/bookings/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // ==================== OVERVIEW STATS ====================

    /**
     * Get overall dashboard statistics
     */
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DashboardOverviewDTO>> getOverview() {
        log.debug("Getting dashboard overview");
        DashboardOverviewDTO overview = dashboardService.getDashboardOverview();
        return ResponseEntity.ok(ApiResponse. success(overview));
    }

    // ==================== BOOKING REPORTS ====================

    /**
     * Get bookings count by status
     */
    @GetMapping("/reports/bookings-by-status")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getBookingsByStatus() {
        log.debug("Getting bookings by status report");
        Map<String, Long> report = dashboardService.getBookingsByStatus();
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * Get bookings count by category
     */
    @GetMapping("/reports/bookings-by-category")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getBookingsByCategory() {
        log.debug("Getting bookings by category report");
        Map<String, Long> report = dashboardService.getBookingsByCategory();
        return ResponseEntity. ok(ApiResponse.success(report));
    }

    /**
     * Get bookings count by date range
     */
    @GetMapping("/reports/bookings-by-date")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getBookingsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.debug("Getting bookings by date report from {} to {}", startDate, endDate);
        Map<String, Long> report = dashboardService. getBookingsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // ==================== TECHNICIAN REPORTS ====================

    /**
     * Get technician workload report
     */
    @GetMapping("/reports/technician-workload")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<TechnicianWorkloadDTO>>> getTechnicianWorkload() {
        log.debug("Getting technician workload report");
        var report = dashboardService.getTechnicianWorkloadReport();
        return ResponseEntity. ok(ApiResponse.success(report));
    }

    /**
     * Get technician performance report
     */
    @GetMapping("/reports/technician-performance")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<TechnicianPerformanceDTO>>> getTechnicianPerformance() {
        log.debug("Getting technician performance report");
        var report = dashboardService.getTechnicianPerformanceReport();
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // ==================== SERVICE TIME REPORTS ====================

    /**
     * Get average service resolution time
     */
    @GetMapping("/reports/avg-resolution-time")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ResolutionTimeReportDTO>> getAverageResolutionTime() {
        log.debug("Getting average resolution time report");
        ResolutionTimeReportDTO report = dashboardService.getResolutionTimeReport();
        return ResponseEntity.ok(ApiResponse. success(report));
    }

    // ==================== CUSTOMER SATISFACTION ====================

    /**
     * Get customer satisfaction / ratings report
     */
    @GetMapping("/reports/customer-satisfaction")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomerSatisfactionDTO>> getCustomerSatisfaction() {
        log.debug("Getting customer satisfaction report");
        CustomerSatisfactionDTO report = dashboardService.getCustomerSatisfactionReport();
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // ==================== MONTHLY REPORTS ====================

    /**
     * Get monthly summary report
     */
    @GetMapping("/reports/monthly-summary")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<MonthlySummaryDTO>> getMonthlySummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        // Default to current month if not specified
        LocalDate now = LocalDate.now();
        int reportYear = year != null ? year :  now.getYear();
        int reportMonth = month != null ? month : now.getMonthValue();

        log.debug("Getting monthly summary for {}/{}", reportMonth, reportYear);
        MonthlySummaryDTO report = dashboardService.getMonthlySummary(reportYear, reportMonth);
        return ResponseEntity. ok(ApiResponse.success(report));
    }
}