package com.app.booking.security;

import io. jsonwebtoken.Claims;
import io. jsonwebtoken. Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans. factory.annotation.Value;
import org. springframework.stereotype.Component;

import java. nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;


@Slf4j
@Component
public class JwtUtil {

    private final Key key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractAllClaims(String token) {
        return Jwts. parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract user ID from JWT token
     * The userId should be stored in a claim called "userId" or "id"
     */
    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);

        // Try to get userId from claims
        Object userId = claims.get("userId");
        if (userId != null) {
            return userId.toString();
        }

        // Fallback:  try "id" claim
        Object id = claims.get("id");
        if (id != null) {
            return id.toString();
        }

        // Last resort: use subject (username) - but this should be avoided
        log.warn("No userId claim found in token, falling back to subject (username). " +
                "This may cause issues with booking lookups!");
        return claims.getSubject();
    }

    public String extractFullName(String token) {
        Claims claims = extractAllClaims(token);

        // Try fullName claim first
        Object fullName = claims.get("fullName");
        if (fullName != null && !fullName.toString().isEmpty()) {
            return fullName.toString();
        }

        // Try to build from firstName + lastName
        Object firstName = claims.get("firstName");
        Object lastName = claims.get("lastName");

        StringBuilder name = new StringBuilder();
        if (firstName != null && ! firstName.toString().isEmpty()) {
            name.append(firstName. toString());
        }
        if (lastName != null && !lastName.toString().isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(lastName.toString());
        }

        if (name.length() > 0) {
            return name. toString();
        }

        // Fallback to username
        return extractUsername(token);
    }

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        Object email = claims.get("email");
        return email != null ? email.toString() : null;
    }

    public String extractPhoneNumber(String token) {
        Claims claims = extractAllClaims(token);
        Object phone = claims.get("phoneNumber");
        if (phone == null) {
            phone = claims.get("phone");
        }
        return phone != null ? phone.toString() : null;
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("roles", List.class);
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            log. warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}