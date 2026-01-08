package com.app.auth.security;

import com.app.auth. model.User;
import com.app.auth.model.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security. Keys;
import lombok.extern. slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtUtils {

    private final Key key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final long tempTokenExpiration;

    public JwtUtils(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration:604800000}") long refreshTokenExpiration,
            @Value("${jwt. temp-token-expiration:300000}") long tempTokenExpiration
    ) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret. getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.tempTokenExpiration = tempTokenExpiration;
    }

    /**
     * Generate access token with full user details
     * This is the UPDATED method - includes userId, fullName, email, phoneNumber
     */
    public String generateAccessToken(User user) {
        String fullName = buildFullName(user.getFirstName(), user.getLastName());

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("fullName", fullName)
                . claim("email", user.getEmail())
                .claim("phoneNumber", user.getPhoneNumber())
                .claim("roles", user.getRoles().stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Generate temporary token (for password reset, etc.)
     */
    public String generateTempToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim("temp", true)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + tempTokenExpiration))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // ==================== EXTRACTION METHODS ====================

    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        try {
            return getClaims(token).getSubject();
        } catch (JwtException e) {
            log.error("Failed to extract username from token", e);
            return null;
        }
    }

    // ==================== VALIDATION METHODS ====================

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired");
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported");
        } catch (MalformedJwtException e) {
            log.error("JWT token is malformed");
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("JWT signature validation failed");
        } catch (IllegalArgumentException e) {
            log.error("JWT token is invalid");
        }
        return false;
    }

    /**
     * Check if token is expired
     */
    public boolean isExpired(String token) {
        try {
            return getClaims(token).getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    /**
     * Get access token expiration time
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * Get refresh token expiration time
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    // ==================== HELPER METHODS ====================

    private Claims getClaims(String token) {
        return Jwts. parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String buildFullName(String firstName, String lastName) {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName. isEmpty()) {
            sb.append(firstName);
        }
        if (lastName != null && !lastName.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(lastName);
        }
        return sb.length() > 0 ? sb.toString() : "User";
    }
}