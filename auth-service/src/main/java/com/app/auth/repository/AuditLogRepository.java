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

    // Delete old logs (for cleanup)
    void deleteByTimestampBefore(Instant cutoffDate);
}
