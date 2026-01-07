package com.app.booking.controller;

import com.app.booking.dto.response.ApiResponse;
import com.app.booking.dto.response.DashboardOverviewDTO;
import com.app.booking.dto.response.ReportResponseDTO;
import com.app.booking.model.ReportType;
import com.app.booking.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerUnitTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    private DashboardOverviewDTO overviewDTO;

    @BeforeEach
    void setUp() {
        overviewDTO = DashboardOverviewDTO.builder()
                .totalBookings(100L)
                .completedBookings(80L)
                .pendingBookings(10L)
                .cancelledBookings(5L)
                .inProgressBookings(5L)
                .build();
    }

    @Test
    void getOverview_ShouldReturnDashboardOverview() {
        when(dashboardService.getDashboardOverview()).thenReturn(overviewDTO);

        ResponseEntity<ApiResponse<DashboardOverviewDTO>> response = dashboardController.getOverview();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getTotalBookings()).isEqualTo(100L);
        verify(dashboardService).getDashboardOverview();
    }

    @Test
    void getAvailableReportTypes_ShouldReturnAllReportTypes() {
        ResponseEntity<ApiResponse<List<Map<String, String>>>> response = dashboardController.getAvailableReportTypes();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotEmpty();
    }

    @Test
    void getReport_StatusReport_ShouldReturnStatusReport() {
        Map<String, Long> statusData = new HashMap<>();
        statusData.put("PENDING", 10L);
        statusData.put("COMPLETED", 80L);
        when(dashboardService.getBookingsByStatus()).thenReturn(statusData);

        ResponseEntity<ApiResponse<ReportResponseDTO<?>>> response =
                dashboardController.getReport(ReportType.STATUS, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(dashboardService).getBookingsByStatus();
    }

    @Test
    void getReport_CategoryReport_ShouldReturnCategoryReport() {
        Map<String, Long> categoryData = new HashMap<>();
        categoryData.put("HVAC", 50L);
        categoryData.put("Plumbing", 30L);
        when(dashboardService.getBookingsByCategory()).thenReturn(categoryData);

        ResponseEntity<ApiResponse<ReportResponseDTO<?>>> response =
                dashboardController.getReport(ReportType.CATEGORY, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(dashboardService).getBookingsByCategory();
    }

    @Test
    void getReport_TechnicianWorkloadReport_ShouldReturnWorkloadReport() {
        when(dashboardService.getTechnicianWorkloadReport()).thenReturn(List.of());

        ResponseEntity<ApiResponse<ReportResponseDTO<?>>> response =
                dashboardController.getReport(ReportType.TECHNICIAN_WORKLOAD, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(dashboardService).getTechnicianWorkloadReport();
    }

    @Test
    void getReport_TechnicianPerformanceReport_ShouldReturnPerformanceReport() {
        when(dashboardService.getTechnicianPerformanceReport()).thenReturn(List.of());

        ResponseEntity<ApiResponse<ReportResponseDTO<?>>> response =
                dashboardController.getReport(ReportType.TECHNICIAN_PERFORMANCE, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(dashboardService).getTechnicianPerformanceReport();
    }

    @Test
    void getReport_ResolutionTimeReport_ShouldReturnResolutionTimeReport() {
        when(dashboardService.getResolutionTimeReport()).thenReturn(null);

        ResponseEntity<ApiResponse<ReportResponseDTO<?>>> response =
                dashboardController.getReport(ReportType.RESOLUTION_TIME, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(dashboardService).getResolutionTimeReport();
    }

    @Test
    void getReport_CustomerSatisfactionReport_ShouldReturnSatisfactionReport() {
        when(dashboardService.getCustomerSatisfactionReport()).thenReturn(null);

        ResponseEntity<ApiResponse<ReportResponseDTO<?>>> response =
                dashboardController.getReport(ReportType.CUSTOMER_SATISFACTION, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(dashboardService).getCustomerSatisfactionReport();
    }

    @Test
    void getReport_MonthlySummaryReport_ShouldReturnMonthlySummary() {
        when(dashboardService.getMonthlySummary(anyInt(), anyInt())).thenReturn(null);

        ResponseEntity<ApiResponse<ReportResponseDTO<?>>> response =
                dashboardController.getReport(ReportType.MONTHLY_SUMMARY, 2026, 1, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(dashboardService).getMonthlySummary(anyInt(), anyInt());
    }
}
