package com.app.auth.security;

import com.app.auth.model.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
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
    private final long tempTokenExpiration;

    public JwtUtils(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.temp-token-expiration:300000}") long tempTokenExpiration
    ) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.tempTokenExpiration = tempTokenExpiration;
    }

    public String generateAccessToken(String username, Set<UserRole> roles) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles.stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateTempToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim("temp", true)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + tempTokenExpiration))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String extractUsername(String token) {
        try {
            return getClaims(token).getSubject();
        } catch (JwtException e) {
            log.error("Failed to extract username from token", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(String token) {
        try {
            Claims claims = getClaims(token);
            return Set.copyOf((java.util.List<String>) claims.get("roles"));
        } catch (Exception e) {
            log.error("Failed to extract roles from token", e);
            return Set.of();
        }
    }

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

    public boolean isExpired(String token) {
        try {
            return getClaims(token).getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}