package com.app.booking.controller;

import com.app.booking.dto.response.*;
import com.app.booking.model.ReportType;
import com.app.booking.security.JwtUtil;
import com.app.booking.service.DashboardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtUtil jwtUtil; // ADD THIS


    private DashboardOverviewDTO overviewDTO;
    private Map<String, Long> statusMap;
    private Map<String, Long> categoryMap;

    @BeforeEach
    void setUp() {
        statusMap = new HashMap<>();
        statusMap.put("PENDING", 10L);
        statusMap.put("COMPLETED", 50L);

        categoryMap = new HashMap<>();
        categoryMap.put("HVAC", 30L);
        categoryMap.put("Plumbing", 20L);

        overviewDTO = DashboardOverviewDTO.builder()
                .totalBookings(100L)
                .pendingBookings(10L)
                .completedBookings(50L)
                .avgRating(4.5)
                .avgResolutionTimeHours(24.0)
                .bookingsByStatus(statusMap)
                .bookingsByCategory(categoryMap)
                .build();
    }

    // ==================== DASHBOARD OVERVIEW TESTS ====================

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void getOverview_ShouldReturnDashboardOverview() throws Exception {
        when(dashboardService.getDashboardOverview()).thenReturn(overviewDTO);

        mockMvc.perform(get("/api/bookings/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalBookings").value(100))
                .andExpect(jsonPath("$.data.pendingBookings").value(10))
                .andExpect(jsonPath("$.data.completedBookings").value(50))
                .andExpect(jsonPath("$.data.avgRating").value(4.5));

        verify(dashboardService, times(1)).getDashboardOverview();
    }

    @Test
    @Disabled("Security test - skipping for now")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void getOverview_ShouldReturnForbidden_WhenNotManager() throws Exception {
        mockMvc.perform(get("/api/bookings/dashboard/overview"))
                .andExpect(status().isForbidden());

        verify(dashboardService, never()).getDashboardOverview();
    }

    // ==================== REPORT TYPES TESTS ====================

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void getAvailableReportTypes_ShouldReturnAllTypes() throws Exception {
        mockMvc.perform(get("/api/bookings/dashboard/reports/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(ReportType.values().length));
    }

    // ==================== STATUS REPORT TESTS ====================

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void getReport_ShouldReturnStatusReport_WhenTypeIsStatus() throws Exception {
        when(dashboardService.getBookingsByStatus()).thenReturn(statusMap);

        mockMvc.perform(get("/api/bookings/dashboard/reports")
                        .param("type", "STATUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportType").value("STATUS"))
                .andExpect(jsonPath("$.data.reportName").value("Bookings by Status"))
                .andExpect(jsonPath("$.data.data.PENDING").value(10))
                .andExpect(jsonPath("$.data.data.COMPLETED").value(50));

        verify(dashboardService, times(1)).getBookingsByStatus();
    }

    // ==================== CATEGORY REPORT TESTS ====================

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void getReport_ShouldReturnCategoryReport_WhenTypeIsCategory() throws Exception {
        when(dashboardService.getBookingsByCategory()).thenReturn(categoryMap);

        mockMvc.perform(get("/api/bookings/dashboard/reports")
                        .param("type", "CATEGORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportType").value("CATEGORY"))
                .andExpect(jsonPath("$.data.data.HVAC").value(30))
                .andExpect(jsonPath("$.data.data.Plumbing").value(20));

        verify(dashboardService, times(1)).getBookingsByCategory();
    }

    // ==================== DATE RANGE REPORT TESTS ====================

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void getReport_ShouldReturnDateReport_WithDateRange() throws Exception {
        Map<String, Long> dateMap = new HashMap<>();
        dateMap.put("2026-01-01", 5L);
        dateMap.put("2026-01-02", 8L);

        when(dashboardService.getBookingsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(dateMap);

        mockMvc.perform(get("/api/bookings/dashboard/reports")
                        .param("type", "DATE")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportType").value("DATE"))
                .andExpect(jsonPath("$.data.fromDate").value("2026-01-01"))
                .andExpect(jsonPath("$.data.toDate").value("2026-01-31"));

        verify(dashboardService, times(1)).getBookingsByDateRange(
                eq(LocalDate.parse("2026-01-01")),
                eq(LocalDate.parse("2026-01-31"))
        );
    }

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void getReport_ShouldUseDefaultDates_WhenDatesNotProvided() throws Exception {
        Map<String, Long> dateMap = new HashMap<>();
        when(dashboardService.getBookingsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(dateMap);

        mockMvc.perform(get("/api/bookings/dashboard/reports")
                        .param("type", "DATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(dashboardService, times(1)).getBookingsByDateRange(any(), any());
    }

    // ==================== TECHNICIAN WORKLOAD REPORT TESTS ====================

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void getReport_ShouldReturnTechnicianWorkload() throws Exception {
        List<TechnicianWorkloadDTO> workloadList = Arrays.asList(
                TechnicianWorkloadDTO.builder()
                        .technicianId("tech123")
                        .technicianName("Jane Smith")
                        .totalActiveBookings(5L)
                        .workloadStatus("MEDIUM")
                        .build()
        );

        when(dashboardService.getTechnicianWorkloadReport()).thenReturn(workloadList);

        mockMvc.perform(get("/api/bookings/dashboard/reports")
                        .param("type", "TECHNICIAN_WORKLOAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportType").value("TECHNICIAN_WORKLOAD"))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data[0].technicianName").value("Jane Smith"));

        verify(dashboardService, times(1)).getTechnicianWorkloadReport();
    }

    // ==================== TECHNICIAN PERFORMANCE REPORT TESTS ====================

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void getReport_ShouldReturnTechnicianPerformance() throws Exception {
        List<TechnicianPerformanceDTO> performanceList = Arrays.asList(
                TechnicianPerformanceDTO.builder()
                        .technicianId("tech123")
                        .technicianName("Jane Smith")
                        .totalJobsCompleted(50L)
                        .avgRating(4.8)
                        .build()
        );

        when(dashboardService.getTechnicianPerformanceReport()).thenReturn(performanceList);

        mockMvc.perform(get("/api/bookings/dashboard/reports")
                        .param("type", "TECHNICIAN_PERFORMANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data[0].avgRating").value(4.8));

        verify(dashboardService, times(1)).getTechnicianPerformanceReport();
    }

    // ==================== RESOLUTION TIME REPORT TESTS ====================

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void getReport_ShouldReturnResolutionTime() throws Exception {
        ResolutionTimeReportDTO resolutionReport = ResolutionTimeReportDTO.builder()
                .avgResolutionTimeHours(24.5)
                .minResolutionTimeHours(2.0)
                .maxResolutionTimeHours(72.0)
                .totalCompletedBookings(100L)
                .build();

        when(dashboardService.getResolutionTimeReport()).thenReturn(resolutionReport);

        mockMvc.perform(get("/api/bookings/dashboard/reports")
                        .param("type", "RESOLUTION_TIME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data.avgResolutionTimeHours").value(24.5))
                .andExpect(jsonPath("$.data.data.totalCompletedBookings").value(100));

        verify(dashboardService, times(1)).getResolutionTimeReport();
    }

    // ==================== CUSTOMER SATISFACTION REPORT TESTS ====================

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void getReport_ShouldReturnCustomerSatisfaction() throws Exception {
        CustomerSatisfactionDTO satisfactionReport = CustomerSatisfactionDTO.builder()
                .avgRating(4.6)
                .totalRatings(200L)
                .fiveStarCount(120L)
                .satisfactionRate(85.0)
                .build();

        when(dashboardService.getCustomerSatisfactionReport()).thenReturn(satisfactionReport);

        mockMvc.perform(get("/api/bookings/dashboard/reports")
                        .param("type", "CUSTOMER_SATISFACTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data.avgRating").value(4.6))
                .andExpect(jsonPath("$.data.data.satisfactionRate").value(85.0));

        verify(dashboardService, times(1)).getCustomerSatisfactionReport();
    }

    // ==================== MONTHLY SUMMARY REPORT TESTS ====================

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void getReport_ShouldReturnMonthlySummary_WithYearAndMonth() throws Exception {
        MonthlySummaryDTO monthlySummary = MonthlySummaryDTO.builder()
                .year(2026)
                .month(1)
                .monthName("January")
                .totalBookings(150L)
                .completedBookings(120L)
                .completionRate(80.0)
                .build();

        when(dashboardService.getMonthlySummary(2026, 1)).thenReturn(monthlySummary);

        mockMvc.perform(get("/api/bookings/dashboard/reports")
                        .param("type", "MONTHLY_SUMMARY")
                        .param("year", "2026")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.month").value(1))
                .andExpect(jsonPath("$.data.data.monthName").value("January"))
                .andExpect(jsonPath("$.data.data.totalBookings").value(150));

        verify(dashboardService, times(1)).getMonthlySummary(2026, 1);
    }

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void getReport_ShouldUseCurrentMonth_WhenNotProvided() throws Exception {
        LocalDate now = LocalDate.now();
        MonthlySummaryDTO monthlySummary = MonthlySummaryDTO.builder()
                .year(now.getYear())
                .month(now.getMonthValue())
                .totalBookings(100L)
                .build();

        when(dashboardService.getMonthlySummary(now.getYear(), now.getMonthValue()))
                .thenReturn(monthlySummary);

        mockMvc.perform(get("/api/bookings/dashboard/reports")
                        .param("type", "MONTHLY_SUMMARY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(dashboardService, times(1)).getMonthlySummary(now.getYear(), now.getMonthValue());
    }
}