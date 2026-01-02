package com.app.booking.model;

public enum ReportType {
    STATUS("Bookings by Status", "Breakdown of bookings by their current status"),
    CATEGORY("Bookings by Category", "Breakdown of bookings by service category"),
    DATE("Bookings by Date", "Bookings count grouped by date"),
    TECHNICIAN_WORKLOAD("Technician Workload", "Current workload distribution among technicians"),
    TECHNICIAN_PERFORMANCE("Technician Performance", "Performance metrics for each technician"),
    RESOLUTION_TIME("Resolution Time", "Average time to complete bookings"),
    CUSTOMER_SATISFACTION("Customer Satisfaction", "Customer ratings and satisfaction metrics"),
    MONTHLY_SUMMARY("Monthly Summary", "Comprehensive monthly booking summary");

    private final String displayName;
    private final String description;

    ReportType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}