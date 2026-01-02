package com.app.booking.controller;

import com.app.booking. dto.response.*;
import com.app.booking. model.ReportType;
import com.app.booking.service.DashboardService;
import lombok. RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework. security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util. Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consolidated Dashboard & Reports Controller
 * Single endpoint for all report types with query parameters
 */
@Slf4j
@RestController
@RequestMapping("/api/bookings/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // ==================== DASHBOARD OVERVIEW ====================

    /**
     * Get overall dashboard statistics
     * Returns a comprehensive overview of all booking metrics
     */
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DashboardOverviewDTO>> getOverview() {
        log.debug("Getting dashboard overview");
        DashboardOverviewDTO overview = dashboardService.getDashboardOverview();
        return ResponseEntity.ok(ApiResponse. success("Dashboard overview retrieved", overview));
    }

    // ==================== AVAILABLE REPORT TYPES ====================

    /**
     * Get list of available report types
     * Helps frontend know what reports are available
     */
    @GetMapping("/reports/types")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getAvailableReportTypes() {
        log.debug("Getting available report types");

        List<Map<String, String>> reportTypes = Arrays.stream(ReportType.values())
                .map(type -> Map.of(
                        "type", type.name(),
                        "displayName", type.getDisplayName(),
                        "description", type.getDescription()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Available report types", reportTypes));
    }

    // ==================== CONSOLIDATED REPORTS ENDPOINT ====================

    /**
     * Get report by type
     *
     * @param type Report type (required): STATUS, CATEGORY, DATE, TECHNICIAN_WORKLOAD,
     *             TECHNICIAN_PERFORMANCE, RESOLUTION_TIME, CUSTOMER_SATISFACTION, MONTHLY_SUMMARY
     * @param year Year for monthly reports (optional, defaults to current year)
     * @param month Month for monthly reports (optional, defaults to current month)
     * @param from Start date for date range reports (optional)
     * @param to End date for date range reports (optional)
     *
     * Examples:
     * - GET /api/bookings/dashboard/reports? type=STATUS
     * - GET /api/bookings/dashboard/reports?type=CATEGORY
     * - GET /api/bookings/dashboard/reports?type=DATE&from=2026-01-01&to=2026-01-31
     * - GET /api/bookings/dashboard/reports?type=MONTHLY_SUMMARY&year=2026&month=1
     * - GET /api/bookings/dashboard/reports?type=TECHNICIAN_WORKLOAD
     * - GET /api/bookings/dashboard/reports?type=TECHNICIAN_PERFORMANCE
     * - GET /api/bookings/dashboard/reports?type=RESOLUTION_TIME
     * - GET /api/bookings/dashboard/reports?type=CUSTOMER_SATISFACTION
     */
    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponseDTO<? >>> getReport(
            @RequestParam ReportType type,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        log.debug("Getting report: type={}, year={}, month={}, from={}, to={}", type, year, month, from, to);

        ReportResponseDTO<?> report = generateReport(type, year, month, from, to);

        return ResponseEntity.ok(ApiResponse.success("Report generated successfully", report));
    }

    // ==================== REPORT GENERATION LOGIC ====================

    private ReportResponseDTO<?> generateReport(ReportType type, Integer year, Integer month,
                                                LocalDate from, LocalDate to) {
        switch (type) {
            case STATUS:
                return generateStatusReport();

            case CATEGORY:
                return generateCategoryReport();

            case DATE:
                return generateDateReport(from, to);

            case TECHNICIAN_WORKLOAD:
                return generateTechnicianWorkloadReport();

            case TECHNICIAN_PERFORMANCE:
                return generateTechnicianPerformanceReport();

            case RESOLUTION_TIME:
                return generateResolutionTimeReport();

            case CUSTOMER_SATISFACTION:
                return generateCustomerSatisfactionReport();

            case MONTHLY_SUMMARY:
                return generateMonthlySummaryReport(year, month);

            default:
                throw new IllegalArgumentException("Unknown report type: " + type);
        }
    }

    // ==================== INDIVIDUAL REPORT GENERATORS ====================

    private ReportResponseDTO<Map<String, Long>> generateStatusReport() {
        log.debug("Generating bookings by status report");
        Map<String, Long> data = dashboardService.getBookingsByStatus();

        return ReportResponseDTO. of(
                ReportType.STATUS.name(),
                ReportType.STATUS.getDisplayName(),
                ReportType.STATUS.getDescription(),
                data
        );
    }

    private ReportResponseDTO<Map<String, Long>> generateCategoryReport() {
        log.debug("Generating bookings by category report");
        Map<String, Long> data = dashboardService.getBookingsByCategory();

        return ReportResponseDTO.of(
                ReportType.CATEGORY.name(),
                ReportType.CATEGORY.getDisplayName(),
                ReportType.CATEGORY.getDescription(),
                data
        );
    }

    private ReportResponseDTO<Map<String, Long>> generateDateReport(LocalDate from, LocalDate to) {
        // Default to last 30 days if not specified
        LocalDate startDate = from != null ? from :  LocalDate.now().minusDays(30);
        LocalDate endDate = to != null ? to : LocalDate.now();

        log.debug("Generating bookings by date report from {} to {}", startDate, endDate);
        Map<String, Long> data = dashboardService. getBookingsByDateRange(startDate, endDate);

        return ReportResponseDTO.ofWithFilters(
                ReportType.DATE.name(),
                ReportType.DATE.getDisplayName(),
                ReportType. DATE.getDescription(),
                data,
                null,
                null,
                startDate.toString(),
                endDate.toString()
        );
    }

    private ReportResponseDTO<List<TechnicianWorkloadDTO>> generateTechnicianWorkloadReport() {
        log.debug("Generating technician workload report");
        List<TechnicianWorkloadDTO> data = dashboardService.getTechnicianWorkloadReport();

        return ReportResponseDTO.of(
                ReportType.TECHNICIAN_WORKLOAD.name(),
                ReportType.TECHNICIAN_WORKLOAD.getDisplayName(),
                ReportType.TECHNICIAN_WORKLOAD.getDescription(),
                data
        );
    }

    private ReportResponseDTO<List<TechnicianPerformanceDTO>> generateTechnicianPerformanceReport() {
        log.debug("Generating technician performance report");
        List<TechnicianPerformanceDTO> data = dashboardService. getTechnicianPerformanceReport();

        return ReportResponseDTO.of(
                ReportType.TECHNICIAN_PERFORMANCE.name(),
                ReportType.TECHNICIAN_PERFORMANCE.getDisplayName(),
                ReportType.TECHNICIAN_PERFORMANCE.getDescription(),
                data
        );
    }

    private ReportResponseDTO<ResolutionTimeReportDTO> generateResolutionTimeReport() {
        log.debug("Generating resolution time report");
        ResolutionTimeReportDTO data = dashboardService.getResolutionTimeReport();

        return ReportResponseDTO.of(
                ReportType. RESOLUTION_TIME.name(),
                ReportType.RESOLUTION_TIME.getDisplayName(),
                ReportType.RESOLUTION_TIME. getDescription(),
                data
        );
    }

    private ReportResponseDTO<CustomerSatisfactionDTO> generateCustomerSatisfactionReport() {
        log.debug("Generating customer satisfaction report");
        CustomerSatisfactionDTO data = dashboardService.getCustomerSatisfactionReport();

        return ReportResponseDTO.of(
                ReportType.CUSTOMER_SATISFACTION.name(),
                ReportType.CUSTOMER_SATISFACTION.getDisplayName(),
                ReportType.CUSTOMER_SATISFACTION.getDescription(),
                data
        );
    }

    private ReportResponseDTO<MonthlySummaryDTO> generateMonthlySummaryReport(Integer year, Integer month) {
        // Default to current month if not specified
        LocalDate now = LocalDate.now();
        int reportYear = year != null ? year : now.getYear();
        int reportMonth = month != null ? month :  now.getMonthValue();

        log.debug("Generating monthly summary report for {}/{}", reportMonth, reportYear);
        MonthlySummaryDTO data = dashboardService.getMonthlySummary(reportYear, reportMonth);

        return ReportResponseDTO.ofWithFilters(
                ReportType.MONTHLY_SUMMARY. name(),
                ReportType. MONTHLY_SUMMARY.getDisplayName(),
                ReportType. MONTHLY_SUMMARY.getDescription(),
                data,
                reportYear,
                reportMonth,
                null,
                null
        );
    }
}
