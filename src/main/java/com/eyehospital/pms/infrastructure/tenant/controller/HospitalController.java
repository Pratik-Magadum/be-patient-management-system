package com.eyehospital.pms.infrastructure.tenant.controller;

import com.eyehospital.pms.common.constant.ApiConstants;
import com.eyehospital.pms.common.response.ApiResponse;
import com.eyehospital.pms.infrastructure.tenant.dto.HospitalResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * REST API contract for hospital / tenant operations.
 *
 * <p>All Swagger / OpenAPI annotations live here, keeping the implementation
 * class free of documentation noise (Single Responsibility principle).</p>
 *
 * <p>Base path: {@code /api/v1/hospitals}</p>
 */
@Tag(name = "Hospital", description = "Hospital tenant lookup operations")
@RequestMapping(ApiConstants.HOSPITALS)
public interface HospitalController {

    /**
     * Retrieves a hospital's public information by its unique subdomain.
     *
     * <p>Only active hospitals are returned. An inactive or unknown subdomain
     * produces a {@code 404 NOT FOUND} response.</p>
     *
     * @param subdomain the tenant subdomain slug (e.g. {@code apollo-eye})
     * @return wrapped {@link HospitalResponseDto} with HTTP {@code 200 OK}
     */
    @GetMapping(ApiConstants.HOSPITAL_BY_SUBDOMAIN)
    @Operation(
            summary     = "Get hospital by subdomain",
            description = "Returns the public profile of an active hospital identified by its unique subdomain slug."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description  = "Hospital found",
                    content      = @Content(schema = @Schema(implementation = HospitalResponseDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description  = "No active hospital found for the given subdomain",
                    content      = @Content(schema = @Schema(hidden = true))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description  = "Unexpected server error",
                    content      = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<ApiResponse<HospitalResponseDto>> getHospitalBySubdomain(
            @Parameter(description = "Unique subdomain slug of the hospital", example = "apollo-eye", required = true)
            @PathVariable String subdomain
    );
}
