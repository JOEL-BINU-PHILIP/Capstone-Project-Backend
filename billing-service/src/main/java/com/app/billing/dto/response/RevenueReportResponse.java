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
    private Double totalRevenue;            // All non-cancelled invoices
    private Double collectedRevenue;        // Paid invoices only
    private Double pendingRevenue;          // Pending invoices

    // Counts
    private Long totalInvoices;
    private Long paidInvoices;
    private Long pendingInvoices;
    private Long cancelledInvoices;

    // Breakdowns
    private Map<String, Double> revenueByCategory;
    private Map<String, Long> invoicesByStatus;

    // Period
    private String periodStart;
    private String periodEnd;
}