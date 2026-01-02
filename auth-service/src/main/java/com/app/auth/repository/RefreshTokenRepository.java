package com.app.auth.repository;

import com.app.auth.model.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserId(String userId);

    // Find all tokens in a token family
    List<RefreshToken> findByTokenFamily(String tokenFamily);

    // Find expired tokens for cleanup
    @Query("{'expiryDate': {$lte: ?0}}")
    List<RefreshToken> findExpiredTokens(Instant now);

    // Count active tokens for a user
    @Query(value = "{'userId': ?0, 'revoked': false, 'expiryDate': {$gt: ?1}}", count = true)
    long countActiveTokensByUserId(String userId, Instant now);
}