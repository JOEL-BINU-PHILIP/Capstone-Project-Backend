package com.app.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time. Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponseDTO<T> {

    private String reportType;
    private String reportName;
    private String description;
    private T data;
    private Instant generatedAt;

    // Optional filters applied
    private Integer year;
    private Integer month;
    private String fromDate;
    private String toDate;

    public static <T> ReportResponseDTO<T> of(String reportType, String reportName, String description, T data) {
        return ReportResponseDTO. <T>builder()
                .reportType(reportType)
                .reportName(reportName)
                .description(description)
                .data(data)
                .generatedAt(Instant.now())
                .build();
    }

    public static <T> ReportResponseDTO<T> ofWithFilters(String reportType, String reportName, String description,
                                                         T data, Integer year, Integer month,
                                                         String fromDate, String toDate) {
        return ReportResponseDTO.<T>builder()
                .reportType(reportType)
                .reportName(reportName)
                .description(description)
                .data(data)
                .generatedAt(Instant.now())
                .year(year)
                .month(month)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();
    }
}