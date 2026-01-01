package com.app.notification.dto.response;

import com.app.notification.model.NotificationStatus;
import com.app.notification.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String id;

    // Recipient
    private String userId;
    private String userEmail;
    private String userName;

    // Content
    private NotificationType type;
    private String title;
    private String message;

    // Reference
    private String referenceId;
    private String referenceType;

    // Status
    private NotificationStatus status;
    private boolean emailSent;
    private boolean isRead;

    // Timestamps
    private Instant createdAt;
    private Instant readAt;
}