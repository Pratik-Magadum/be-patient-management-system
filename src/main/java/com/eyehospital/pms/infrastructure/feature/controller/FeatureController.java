package com.eyehospital.pms.infrastructure.feature.controller;

import java.util.Map;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.eyehospital.pms.common.constants.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST API contract for feature-flag operations.
 *
 * <p>All Swagger / OpenAPI annotations live here, keeping the implementation
 * class free of documentation noise (Single Responsibility principle).</p>
 *
 * <p>Base path: {@code /api/v1/features}</p>
 */
@Tag(name = "Feature", description = "Feature flag operations")
@RequestMapping(ApiConstants.FEATURES)
public interface FeatureController {

    /**
     * Returns feature flags for the authenticated user's role.
     *
     * @param authentication the JWT authentication token (injected by Spring Security)
     * @return map of feature key → enabled status for the caller's role
     */
    @GetMapping
    @Operation(
            summary     = "Get feature flags for current user",
            description = "Returns a map of feature keys and their enabled/disabled status based on the authenticated user's role from the JWT token."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description  = "Feature flags retrieved successfully",
                    content      = @Content(schema = @Schema(implementation = Map.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description  = "Unauthorized — missing or invalid JWT token",
                    content      = @Content(schema = @Schema(hidden = true))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description  = "Unexpected server error",
                    content      = @Content(schema = @Schema(hidden = true))
            )
    })
    Map<String, Boolean> getFeatures(JwtAuthenticationToken authentication);
}
