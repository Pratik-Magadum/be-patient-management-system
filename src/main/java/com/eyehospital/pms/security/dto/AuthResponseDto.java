package com.eyehospital.pms.security.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Authentication response containing tokens and expiry info")
public class AuthResponseDto {

    @Schema(description = "JWT access token", example = "eyJhbGci...")
    private final String accessToken;

    @Schema(description = "Refresh token to obtain new access token", example = "eyJhbGci...")
    private final String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    private final String tokenType;

    /**
     * Access token expiry in EPOCH milliseconds.
     * Frontend uses this to schedule proactive refresh before expiry.
     * Example: Date.now() + (expiresIn * 1000)
     */
    @Schema(description = "Access token expiry in milliseconds from now", example = "900000")
    private final long expiresIn;

    @Schema(description = "Logged-in user's role", example = "ADMIN")
    private final String role;

    @Schema(description = "Hospital ID for tenant context", example = "a0eebc99-...")
    private final String hospitalId;
}
