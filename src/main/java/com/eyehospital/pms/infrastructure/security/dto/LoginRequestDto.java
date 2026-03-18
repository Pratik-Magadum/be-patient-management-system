package com.eyehospital.pms.infrastructure.security.dto;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Login request payload")
public class LoginRequestDto {

    @NotNull(message = "Hospital ID is required")
    @Schema(example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11", description = "UUID of the hospital the user belongs to")
    private UUID hospitalId;

    @NotBlank(message = "Username is required")
    @Schema(example = "admin")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(example = "password123")
    private String password;
}
