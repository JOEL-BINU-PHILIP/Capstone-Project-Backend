package com.app. notification.controller;

import com.app. notification.dto.request.SendNotificationRequest;
import com. app.notification.dto.response.ApiResponse;
import com.app.notification. dto.response.NotificationResponse;
import com.app.notification.model.NotificationType;
import com.app.notification.service. NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok. extern.slf4j.Slf4j;
import org. springframework.data.domain.Page;
import org.springframework.data.domain. Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework. security.access.prepost.PreAuthorize;
import org. springframework.security.core.Authentication;
import org.springframework.security.core. GrantedAuthority;
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
        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity. status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notification sent", response));
    }

    // ==================== LIST (CONSOLIDATED) ====================

    /**
     * Get notifications with optional filters
     *
     * Usage examples:
     * - GET /api/notifications                              -> All notifications (Admin only)
     * - GET /api/notifications?user=me                      -> My notifications
     * - GET /api/notifications?user=me&read=false           -> My unread notifications
     * - GET /api/notifications? userId={id}                  -> By user ID (Admin/Manager)
     * - GET /api/notifications?type=BOOKING_CREATED         -> By type (Admin)
     * - GET /api/notifications? user=me&type=INVOICE_GENERATED -> Combined filters
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) Boolean read,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        String currentUser = authentication. getName();
        boolean isAdmin = isAdmin(authentication);
        boolean isManagerOrAdmin = isManagerOrAdmin(authentication);

        // Determine effective user ID
        String effectiveUserId = null;

        if ("me".equalsIgnoreCase(user)) {
            // User wants their own notifications
            effectiveUserId = currentUser;
        } else if (userId != null) {
            // Admin/Manager filtering by specific user
            if (! isManagerOrAdmin) {
                log.warn("User {} attempted to view notifications for user {}", currentUser, userId);
                effectiveUserId = currentUser;
            } else {
                effectiveUserId = userId;
            }
        } else if (! isAdmin) {
            // Non-admins with no filter should only see their own notifications
            effectiveUserId = currentUser;
        }

        // Type filter is only available for admins unless combined with user=me
        if (type != null && effectiveUserId == null && !isAdmin) {
            effectiveUserId = currentUser;
        }

        Page<NotificationResponse> response = notificationService. getNotifications(
                effectiveUserId,
                type,
                read,
                currentUser,
                isAdmin,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== COUNT (CONSOLIDATED) ====================

    /**
     * Get notification count with optional filters
     *
     * Usage examples:
     * - GET /api/notifications/count                        -> Total count (Admin)
     * - GET /api/notifications/count?read=false             -> My unread count
     * - GET /api/notifications/count? user=me&read=false     -> My unread count (explicit)
     * - GET /api/notifications/count?type=BOOKING_CREATED   -> Count by type (Admin)
     */
    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getNotificationCount(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) Boolean read,
            Authentication authentication
    ) {
        String currentUser = authentication. getName();
        boolean isAdmin = isAdmin(authentication);
        boolean isManagerOrAdmin = isManagerOrAdmin(authentication);

        // Determine effective user ID
        String effectiveUserId = null;

        if ("me".equalsIgnoreCase(user)) {
            effectiveUserId = currentUser;
        } else if (userId != null) {
            if (!isManagerOrAdmin) {
                effectiveUserId = currentUser;
            } else {
                effectiveUserId = userId;
            }
        } else if (! isAdmin) {
            // Non-admins should only count their own notifications
            effectiveUserId = currentUser;
        }

        long count = notificationService.getNotificationCount(
                effectiveUserId,
                type,
                read,
                currentUser,
                isAdmin
        );

        return ResponseEntity.ok(ApiResponse.success(Map. of("count", count)));
    }

    // ==================== GET BY ID ====================

    /**
     * Get notification by ID
     */
    @GetMapping("/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(
            @PathVariable String notificationId
    ) {
        NotificationResponse notification = notificationService. getNotificationById(notificationId);
        return ResponseEntity. ok(ApiResponse. success(notification));
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
        NotificationResponse response = notificationService. markAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", response));
    }

    /**
     * Mark all notifications as read
     */
    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            Authentication authentication
    ) {
        String userId = authentication.getName();
        notificationService.markAllAsRead(userId);
        return ResponseEntity. ok(ApiResponse. success("All notifications marked as read", null));
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
        return ResponseEntity. ok(ApiResponse. success("Notification deleted", null));
    }

    // ==================== DEPRECATED ENDPOINTS (Keep for backward compatibility) ====================

    /**
     * @deprecated Use GET /api/notifications? user=me instead
     */
    @Deprecated
    @GetMapping("/my-notifications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            Authentication authentication
    ) {
        String userId = authentication. getName();
        List<NotificationResponse> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * @deprecated Use GET /api/notifications? user=me with pagination instead
     */
    @Deprecated
    @GetMapping("/my-notifications/paged")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotificationsPaged(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort. Direction.DESC) Pageable pageable
    ) {
        String userId = authentication. getName();
        Page<NotificationResponse> notifications = notificationService.getUserNotificationsPaged(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * @deprecated Use GET /api/notifications? user=me&read=false instead
     */
    @Deprecated
    @GetMapping("/my-notifications/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyUnreadNotifications(
            Authentication authentication
    ) {
        String userId = authentication.getName();
        List<NotificationResponse> notifications = notificationService. getUnreadNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * @deprecated Use GET /api/notifications/count?read=false instead
     */
    @Deprecated
    @GetMapping("/my-notifications/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            Authentication authentication
    ) {
        String userId = authentication.getName();
        long count = notificationService. getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    /**
     * @deprecated Use GET /api/notifications? type={type} instead
     */
    @Deprecated
    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsByType(
            @PathVariable NotificationType type
    ) {
        List<NotificationResponse> notifications = notificationService.getNotificationsByType(type);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * @deprecated Use GET /api/notifications? userId={userId} instead
     */
    @Deprecated
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('SERVICE_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(
            @PathVariable String userId
    ) {
        List<NotificationResponse> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity. ok(ApiResponse. success(notifications));
    }

    /**
     * @deprecated Use POST /api/notifications/read-all instead
     */
    @Deprecated
    @PostMapping("/my-notifications/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAllAsReadLegacy(
            Authentication authentication
    ) {
        String userId = authentication.getName();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ADMIN"));
    }

    private boolean isManagerOrAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_SERVICE_MANAGER") ||
                        role.equals("ROLE_ADMIN") ||
                        role.equals("SERVICE_MANAGER") ||
                        role.equals("ADMIN"));
    }
}