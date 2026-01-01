package com. app.notification.service;

import com.app.notification.dto.request.SendNotificationRequest;
import com.app.notification.dto.response.NotificationResponse;
import com.app.notification.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain. Pageable;

import java.util.List;

public interface NotificationService {

    // Send notification
    NotificationResponse sendNotification(SendNotificationRequest request);

    // Quick send methods for common notifications
    NotificationResponse sendBookingCreatedNotification(String userId, String userEmail, String userName, String bookingId, String bookingNumber);
    NotificationResponse sendTechnicianAssignedNotification(String userId, String userEmail, String userName, String bookingId, String technicianName);
    NotificationResponse sendServiceCompletedNotification(String userId, String userEmail, String userName, String bookingId);
    NotificationResponse sendInvoiceGeneratedNotification(String userId, String userEmail, String userName, String invoiceId, String invoiceNumber, Double amount);
    NotificationResponse sendPaymentReceivedNotification(String userId, String userEmail, String userName, String invoiceNumber, Double amount);

    // Read
    NotificationResponse getNotificationById(String notificationId);

    // List
    List<NotificationResponse> getUserNotifications(String userId);
    Page<NotificationResponse> getUserNotificationsPaged(String userId, Pageable pageable);
    List<NotificationResponse> getUnreadNotifications(String userId);
    long getUnreadCount(String userId);

    // Mark as read
    NotificationResponse markAsRead(String notificationId, String userId);
    void markAllAsRead(String userId);

    // Delete
    void deleteNotification(String notificationId, String userId);

    // Admin
    List<NotificationResponse> getNotificationsByType(NotificationType type);
    Page<NotificationResponse> getAllNotificationsPaged(Pageable pageable);
}