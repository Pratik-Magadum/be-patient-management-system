package com.eyehospital.pms.infrastructure.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Refresh token request payload")
public class RefreshTokenRequestDto {

    @NotBlank(message = "Refresh token is required")
    @Schema(description = "The refresh token issued during login")
    private String refreshToken;
}
