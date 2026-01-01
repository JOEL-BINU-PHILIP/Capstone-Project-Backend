package com.app.booking.service;

import com.app.booking.dto.response.*;

import java.time.LocalDate;
import java. util.List;
import java. util.Map;

public interface DashboardService {

    // Overview
    DashboardOverviewDTO getDashboardOverview();

    // Booking reports
    Map<String, Long> getBookingsByStatus();
    Map<String, Long> getBookingsByCategory();
    Map<String, Long> getBookingsByDateRange(LocalDate startDate, LocalDate endDate);

    // Technician reports
    List<TechnicianWorkloadDTO> getTechnicianWorkloadReport();
    List<TechnicianPerformanceDTO> getTechnicianPerformanceReport();

    // Service time
    ResolutionTimeReportDTO getResolutionTimeReport();

    // Customer satisfaction
    CustomerSatisfactionDTO getCustomerSatisfactionReport();

    // Monthly
    MonthlySummaryDTO getMonthlySummary(int year, int month);
}