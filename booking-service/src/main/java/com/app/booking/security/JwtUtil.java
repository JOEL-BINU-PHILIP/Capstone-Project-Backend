package com. app.booking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken. Jwts;
import io. jsonwebtoken.security.Keys;
import lombok.extern.slf4j. Slf4j;
import org. springframework.beans.factory.annotation. Value;
import org.springframework. stereotype.Component;

import java. nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtUtil {

    private final Key key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extract all claims from token
     */
    public Claims extractAllClaims(String token) {
        return Jwts. parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extract username (subject) from token
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract user ID from token
     */
    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);

        // Try to get userId from claims
        Object userId = claims.get("userId");
        if (userId != null) {
            return userId.toString();
        }

        // Try id
        Object id = claims.get("id");
        if (id != null) {
            return id.toString();
        }

        // Fallback to subject (username)
        return claims.getSubject();
    }

    /**
     * Extract full name from token
     */
    public String extractFullName(String token) {
        Claims claims = extractAllClaims(token);

        // Try fullName
        Object fullName = claims.get("fullName");
        if (fullName != null) {
            return fullName.toString();
        }

        // Try name
        Object name = claims.get("name");
        if (name != null) {
            return name.toString();
        }

        // Try firstName + lastName
        Object firstName = claims. get("firstName");
        Object lastName = claims.get("lastName");
        if (firstName != null || lastName != null) {
            String first = firstName != null ? firstName.toString() : "";
            String last = lastName != null ? lastName.toString() : "";
            return (first + " " + last).trim();
        }

        // Fallback to username
        return claims.getSubject();
    }

    /**
     * Extract email from token
     */
    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        Object email = claims.get("email");
        return email != null ? email.toString() : null;
    }

    /**
     * Extract phone number from token
     */
    public String extractPhoneNumber(String token) {
        Claims claims = extractAllClaims(token);

        // Try phoneNumber
        Object phone = claims.get("phoneNumber");
        if (phone != null) {
            return phone.toString();
        }

        // Try phone
        phone = claims.get("phone");
        if (phone != null) {
            return phone.toString();
        }

        // Try mobile
        phone = claims.get("mobile");
        return phone != null ? phone.toString() : null;
    }

    /**
     * Extract roles from token
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractAllClaims(token).get("roles", List.class);
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return ! isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token with username check
     */
    public boolean validateToken(String token, String username) {
        try {
            String extractedUsername = extractUsername(token);
            return extractedUsername. equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}