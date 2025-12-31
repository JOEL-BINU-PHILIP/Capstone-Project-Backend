package com.app.auth.repository;

import com.app.auth.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    // Find logs by user
    Page<AuditLog> findByUserId(String userId, Pageable pageable);

    // Find logs by action
    Page<AuditLog> findByAction(AuditLog.AuditAction action, Pageable pageable);

    // Find logs by IP address
    List<AuditLog> findByIpAddress(String ipAddress);

    // Find failed login attempts
    @Query("{'action': 'LOGIN_FAILED', 'timestamp': {$gte: ?0}}")
    List<AuditLog> findFailedLoginsAfter(Instant after);

    // Find logs in date range
    @Query("{'timestamp': {$gte: ?0, $lte: ?1}}")
    Page<AuditLog> findByTimestampBetween(Instant start, Instant end, Pageable pageable);

    // Find suspicious activity (multiple failed logins)
    @Query("{'userId': ?0, 'action': 'LOGIN_FAILED', 'timestamp': {$gte: ?1}}")
    List<AuditLog> findFailedLoginsByUserSince(String userId, Instant since);

    // Count by action and success
    long countByActionAndSuccess(AuditLog.AuditAction action, boolean success);

    // Delete old logs (for cleanup)
    void deleteByTimestampBefore(Instant cutoffDate);
}
