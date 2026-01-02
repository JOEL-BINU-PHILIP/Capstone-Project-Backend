package com.app.notification. repository;

import com.app.notification. model.Notification;
import com.app.notification. model.NotificationStatus;
import com.app.notification. model.NotificationType;
import org. springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data. mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository. Query;

import java.time. Instant;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    // Find by user
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    Page<Notification> findByUserId(String userId, Pageable pageable);

    // Find unread notifications for user
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);
    Page<Notification> findByUserIdAndIsReadFalse(String userId, Pageable pageable);

    // ========== NEW:  Combined Filters ==========

    // By user AND read status
    Page<Notification> findByUserIdAndIsRead(String userId, boolean isRead, Pageable pageable);

    // By user AND type
    Page<Notification> findByUserIdAndType(String userId, NotificationType type, Pageable pageable);

    // By user AND type AND read status
    Page<Notification> findByUserIdAndTypeAndIsRead(String userId, NotificationType type, boolean isRead, Pageable pageable);

    // By type only (for admin)
    Page<Notification> findByType(NotificationType type, Pageable pageable);

    // By type AND read status (for admin)
    Page<Notification> findByTypeAndIsRead(NotificationType type, boolean isRead, Pageable pageable);

    // By read status only (for admin)
    Page<Notification> findByIsRead(boolean isRead, Pageable pageable);

    // Find by status
    List<Notification> findByStatus(NotificationStatus status);

    // Find by type (list)
    List<Notification> findByType(NotificationType type);

    // Find by reference
    List<Notification> findByReferenceIdAndReferenceType(String referenceId, String referenceType);

    // Count unread for user
    long countByUserIdAndIsReadFalse(String userId);

    // ========== NEW: Count with filters ==========
    long countByUserIdAndIsRead(String userId, boolean isRead);
    long countByIsRead(boolean isRead);
    long countByType(NotificationType type);
    long countByUserIdAndType(String userId, NotificationType type);

    // Count by status
    long countByStatus(NotificationStatus status);

    // Find notifications in date range
    @Query("{'createdAt': {$gte: ? 0, $lte:  ?1}}")
    List<Notification> findByCreatedAtBetween(Instant start, Instant end);

    // Find failed notifications for retry
    List<Notification> findByStatusAndEmailSentFalse(NotificationStatus status);
}