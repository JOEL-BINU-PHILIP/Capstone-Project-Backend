package com.app.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org. springframework.data.mongodb.core. mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    // Recipient
    @Indexed
    private String userId;
    private String userEmail;
    private String userName;
    private String userRole;            // CUSTOMER, TECHNICIAN, SERVICE_MANAGER

    // Notification Content
    private NotificationType type;
    private String title;
    private String message;

    // Reference (optional - links to related entity)
    private String referenceId;         // bookingId, invoiceId, etc.
    private String referenceType;       // BOOKING, INVOICE, etc.

    // Status
    @Indexed
    private NotificationStatus status;

    // Delivery
    private boolean emailSent;
    private Instant emailSentAt;
    private String emailError;          // Error message if email failed

    // Read status (for in-app notifications)
    private boolean isRead;
    private Instant readAt;

    // Audit
    @CreatedDate
    @Indexed
    private Instant createdAt;
}