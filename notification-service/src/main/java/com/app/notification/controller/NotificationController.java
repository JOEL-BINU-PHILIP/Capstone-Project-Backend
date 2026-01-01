package com.app.notification.controller;

import com.app.notification.dto.request.SendNotificationRequest;
import com.app.notification.dto. response.ApiResponse;
import com.app. notification.dto.response.NotificationResponse;
import com.app.notification.model.NotificationType;
import com.app.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org. springframework.data.domain.Page;
import org.springframework.data. domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ==================== SEND NOTIFICATION ====================

    /**
     * Send a notification (internal use or admin)
     */
    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request
    ) {
        NotificationResponse response = notificationService. sendNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse. success("Notification sent", response));
    }

    // ==================== USER NOTIFICATIONS ====================

    /**
     * Get my notifications
     */
    @GetMapping("/my-notifications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            Authentication authentication
    ) {
        String userId = authentication.getName();
        List<NotificationResponse> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity. ok(ApiResponse.success(notifications));
    }

    /**
     * Get my notifications (paginated)
     */
    @GetMapping("/my-notifications/paged")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotificationsPaged(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String userId = authentication. getName();
        Page<NotificationResponse> notifications = notificationService.getUserNotificationsPaged(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * Get my unread notifications
     */
    @GetMapping("/my-notifications/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyUnreadNotifications(
            Authentication authentication
    ) {
        String userId = authentication.getName();
        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(ApiResponse. success(notifications));
    }

    /**
     * Get unread count
     */
    @GetMapping("/my-notifications/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            Authentication authentication
    ) {
        String userId = authentication.getName();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse. success(Map.of("count", count)));
    }

    /**
     * Get notification by ID
     */
    @GetMapping("/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(
            @PathVariable String notificationId
    ) {
        NotificationResponse notification = notificationService.getNotificationById(notificationId);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    // ==================== MARK AS READ ====================

    /**
     * Mark a notification as read
     */
    @PostMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable String notificationId,
            Authentication authentication
    ) {
        String userId = authentication.getName();
        NotificationResponse response = notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", response));
    }

    /**
     * Mark all notifications as read
     */
    @PostMapping("/my-notifications/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            Authentication authentication
    ) {
        String userId = authentication.getName();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    // ==================== DELETE ====================

    /**
     * Delete a notification
     */
    @DeleteMapping("/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable String notificationId,
            Authentication authentication
    ) {
        String userId = authentication.getName();
        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }

    // ==================== ADMIN ====================

    /**
     * Get all notifications (admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getAllNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<NotificationResponse> notifications = notificationService.getAllNotificationsPaged(pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * Get notifications by type (admin only)
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsByType(
            @PathVariable NotificationType type
    ) {
        List<NotificationResponse> notifications = notificationService.getNotificationsByType(type);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * Get notifications for a specific user (admin/manager)
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(
            @PathVariable String userId
    ) {
        List<NotificationResponse> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity. ok(ApiResponse.success(notifications));
    }
}