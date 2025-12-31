package com.app.auth.repository;

import com.app.auth.model.User;
import com.app.auth.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByEmailVerificationToken(String token);

    Optional<User> findByPasswordResetToken(String token);

    // Find users by role
    @Query("{'roles': ?0}")
    List<User> findByRole(UserRole role);

    @Query("{'roles': ?0}")
    Page<User> findByRole(UserRole role, Pageable pageable);

    // Find locked accounts
    @Query("{'accountNonLocked': false}")
    List<User> findLockedAccounts();

    // Find users with failed login attempts
    @Query("{'failedLoginAttempts': {$gte: ?0}}")
    List<User> findUsersWithFailedAttempts(int minAttempts);

    // Find users who need email verification
    @Query("{'emailVerified': false, 'createdAt': {$lte: ?0}}")
    List<User> findUnverifiedUsersOlderThan(Instant cutoffDate);

    // Find by service assignment (for service managers)
    Optional<User> findByAssignedServiceId(String serviceId);

    List<User> findAllByAssignedServiceId(String serviceId);
}
