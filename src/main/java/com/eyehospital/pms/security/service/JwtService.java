package com.eyehospital.pms.security.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import com.eyehospital.pms.security.config.OAuth2Properties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT service backed by Spring Security OAuth2 JOSE (Nimbus).
 * Uses RSA-256 signing via {@link JwtEncoder} and verification via {@link JwtDecoder}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final OAuth2Properties oauth2Properties;

    /**
     * Generates a short-lived ACCESS token (15 min by default).
     * Claims include: userId, hospitalId, role, tokenType — used for authorization and tenant context.
     */
    public String generateAccessToken(UUID userId, String username, UUID hospitalId, String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(oauth2Properties.getIssuer())
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plusMillis(oauth2Properties.getAccessTokenExpiryMs()))
                .claim("userId", userId.toString())
                .claim("hospitalId", hospitalId.toString())
                .claim("role", role)
                .claim("tokenType", "ACCESS")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * Generates a long-lived REFRESH token (7 days by default).
     */
    public String generateRefreshToken(UUID userId, String username) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(oauth2Properties.getIssuer())
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plusMillis(oauth2Properties.getRefreshTokenExpiryMs()))
                .claim("userId", userId.toString())
                .claim("tokenType", "REFRESH")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * Decodes and validates a JWT token, returning the parsed {@link Jwt}.
     * @throws JwtException if the token is expired, tampered, or otherwise invalid
     */
    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    public String extractUsername(String token) {
        return decode(token).getSubject();
    }

    public String extractHospitalId(String token) {
        return decode(token).getClaimAsString("hospitalId");
    }

    public String extractRole(String token) {
        return decode(token).getClaimAsString("role");
    }

    /** Returns true if token is valid and NOT expired. */
    public boolean isTokenValid(String token) {
        try {
            decode(token);
            return true;
        } catch (JwtException e) {
            log.debug("JWT token is invalid or expired: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("JWT token validation error: {}", e.getMessage());
            return false;
        }
    }

    public long getAccessTokenExpiryMs() {
        return oauth2Properties.getAccessTokenExpiryMs();
    }
}