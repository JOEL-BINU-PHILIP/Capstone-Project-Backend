package com.app.notification.model;

public enum NotificationStatus {
    PENDING,        // Created but not sent
    SENT,           // Successfully sent
    FAILED,         // Failed to send
    READ            // Read by user (for in-app notifications)
}