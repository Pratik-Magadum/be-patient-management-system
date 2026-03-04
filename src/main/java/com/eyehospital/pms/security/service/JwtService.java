package com.eyehospital.pms.security.service;

import com.eyehospital.pms.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    /**
     * Generates a short-lived ACCESS token (15 min by default).
     * Claims include: hospitalId, role — used by SecurityFilter for tenant context.
     */
    public String generateAccessToken(UUID userId, String username, UUID hospitalId, String role) {
        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        "userId", userId.toString(),
                        "hospitalId", hospitalId.toString(),
                        "role", role,
                        "tokenType", "ACCESS"
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiryMs()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generates a long-lived REFRESH token (7 days by default).
     */
    public String generateRefreshToken(UUID userId, String username) {
        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        "userId", userId.toString(),
                        "tokenType", "REFRESH"
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpiryMs()))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractHospitalId(String token) {
        return extractAllClaims(token).get("hospitalId", String.class);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /** Returns true if token is valid and NOT expired */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token is expired: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("JWT token is invalid: {}", e.getMessage());
            return false;
        }
    }

    /** Returns true ONLY if token is expired (used for refresh flow) */
    public boolean isTokenExpired(String token) {
        try {
            extractAllClaims(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true; // ← Frontend gets 401 because of this
        }
    }

    public long getAccessTokenExpiryMs() {
        return jwtProperties.getAccessTokenExpiryMs();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}