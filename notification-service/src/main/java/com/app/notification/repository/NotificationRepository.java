package com.app. notification.repository;

import com. app.notification.model.Notification;
import com.app.notification.model.NotificationStatus;
import com.app.notification.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    // Find by user
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    Page<Notification> findByUserId(String userId, Pageable pageable);

    // Find unread notifications for user
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);

    // Find by status
    List<Notification> findByStatus(NotificationStatus status);

    // Find by type
    List<Notification> findByType(NotificationType type);

    // Find by reference
    List<Notification> findByReferenceIdAndReferenceType(String referenceId, String referenceType);

    // Count unread for user
    long countByUserIdAndIsReadFalse(String userId);

    // Count by status
    long countByStatus(NotificationStatus status);

    // Find notifications in date range
    @Query("{'createdAt': {$gte: ?0, $lte: ?1}}")
    List<Notification> findByCreatedAtBetween(Instant start, Instant end);

    // Find failed notifications for retry
    List<Notification> findByStatusAndEmailSentFalse(NotificationStatus status);
}