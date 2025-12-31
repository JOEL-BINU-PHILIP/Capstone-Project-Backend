package com.app.auth.repository;

import com.app.auth.model.LoginAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;

public interface LoginAttemptRepository extends MongoRepository<LoginAttempt, String> {

    // Count failed attempts by username in time window
    @Query(value = "{'username': ?0, 'success': false, 'timestamp': {$gte: ?1}}", count = true)
    long countFailedAttemptsByUsername(String username, Instant since);

    // Count failed attempts by IP in time window
    @Query(value = "{'ipAddress': ?0, 'success': false, 'timestamp': {$gte: ?1}}", count = true)
    long countFailedAttemptsByIp(String ipAddress, Instant since);

    // Find recent attempts by username
    @Query("{'username': ?0, 'timestamp': {$gte: ?1}}")
    List<LoginAttempt> findRecentAttemptsByUsername(String username, Instant since);

    // Find recent attempts by IP
    @Query("{'ipAddress': ?0, 'timestamp': {$gte: ?1}}")
    List<LoginAttempt> findRecentAttemptsByIp(String ipAddress, Instant since);

    // Cleanup old attempts (happens automatically via TTL index)
    void deleteByTimestampBefore(Instant cutoffDate);
}