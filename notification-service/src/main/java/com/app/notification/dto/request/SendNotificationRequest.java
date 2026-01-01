package com.app.notification.dto. request;

import com.app.notification.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    // Recipient
    @NotBlank(message = "User ID is required")
    private String userId;

    private String userEmail;
    private String userName;
    private String userRole;

    // Content
    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    // Reference (optional)
    private String referenceId;
    private String referenceType;

    // Options
    @Builder.Default
    private boolean sendEmail = true;
}