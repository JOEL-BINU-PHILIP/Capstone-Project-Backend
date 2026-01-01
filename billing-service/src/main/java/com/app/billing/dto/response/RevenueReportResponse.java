package com.app.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportResponse {

    // Summary
    private Double totalRevenue;
    private Double totalPaid;
    private Double totalPending;
    private Double totalOverdue;

    // Counts
    private Long totalInvoices;
    private Long paidInvoices;
    private Long pendingInvoices;
    private Long overdueInvoices;

    // Breakdowns
    private Map<String, Double> revenueByMonth;
    private Map<String, Double> revenueByCategory;
    private Map<String, Double> revenueByPaymentMethod;

    // Period
    private String periodStart;
    private String periodEnd;
}