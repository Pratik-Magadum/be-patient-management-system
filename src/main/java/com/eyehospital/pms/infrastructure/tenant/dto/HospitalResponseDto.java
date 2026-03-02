package com.eyehospital.pms.infrastructure.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outgoing DTO returned by the hospital lookup endpoint.
 *
 * <p>Deliberately separate from any request DTO — the response contains
 * server-generated fields ({@code hospitalId}, timestamps) that the caller
 * must never be allowed to set.</p>
 */
@Getter
@Builder
@Schema(description = "Hospital information returned by the API")
public class HospitalResponseDto {

    @Schema(description = "Unique hospital identifier (UUID)",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private final UUID hospitalId;

    @Schema(description = "Hospital display name", example = "Apollo Eye Hospital")
    private final String name;

    @Schema(description = "Unique subdomain slug used for tenant resolution",
            example = "apollo-eye")
    private final String subdomain;

    @Schema(description = "Hospital physical address",
            example = "12, Park Street, Kolkata - 700016")
    private final String address;

    @Schema(description = "Primary contact email", example = "contact@apollo-eye.com")
    private final String contactEmail;

    @Schema(description = "Primary contact phone number", example = "+91 9800000001")
    private final String contactPhone;

    @Schema(description = "Whether the hospital account is active", example = "true")
    private final boolean active;

    @Schema(description = "Record creation timestamp", example = "2026-03-02T10:00:00")
    private final LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2026-03-02T11:00:00")
    private final LocalDateTime updatedAt;
}
