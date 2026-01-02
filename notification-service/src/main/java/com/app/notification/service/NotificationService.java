package com.app. notification.service;

import com.app. notification.dto.request.SendNotificationRequest;
import com.app. notification.dto.response.NotificationResponse;
import com.app.notification.model. NotificationType;
import org.springframework.data. domain.Page;
import org.springframework. data.domain. Pageable;

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

    // ========== NEW:  Unified List Method ==========
    /**
     * Get notifications with optional filters
     * @param userId - Filter by user ID (null for all - admin only)
     * @param type - Filter by notification type (null for all)
     * @param isRead - Filter by read status (null for all)
     * @param currentUser - Current authenticated user
     * @param isAdmin - Whether the current user is admin
     * @param pageable - Pagination parameters
     */
    Page<NotificationResponse> getNotifications(
            String userId,
            NotificationType type,
            Boolean isRead,
            String currentUser,
            boolean isAdmin,
            Pageable pageable
    );

    // ========== NEW:  Unified Count Method ==========
    /**
     * Get notification count with optional filters
     */
    long getNotificationCount(
            String userId,
            NotificationType type,
            Boolean isRead,
            String currentUser,
            boolean isAdmin
    );

    // ========== KEEP: Legacy methods for backward compatibility (can be deprecated) ==========
    List<NotificationResponse> getUserNotifications(String userId);
    Page<NotificationResponse> getUserNotificationsPaged(String userId, Pageable pageable);
    List<NotificationResponse> getUnreadNotifications(String userId);
    long getUnreadCount(String userId);

    // Mark as read
    NotificationResponse markAsRead(String notificationId, String userId);
    void markAllAsRead(String userId);

    // Delete
    void deleteNotification(String notificationId, String userId);

    // Admin (deprecated - use getNotifications with filters)
    List<NotificationResponse> getNotificationsByType(NotificationType type);
    Page<NotificationResponse> getAllNotificationsPaged(Pageable pageable);
}