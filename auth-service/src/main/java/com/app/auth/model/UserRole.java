package com.app.auth.model;

public enum UserRole {
    ROLE_ADMIN("Admin", "Full system access"),
    ROLE_SERVICE_MANAGER("Service Manager", "Manages technicians and bookings for a service"),
    ROLE_TECHNICIAN("Technician", "Performs services and updates job status"),
    ROLE_CUSTOMER("Customer", "Books services and provides feedback");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
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
