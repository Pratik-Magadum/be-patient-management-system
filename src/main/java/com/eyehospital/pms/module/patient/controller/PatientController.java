package com.eyehospital.pms.module.patient.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.eyehospital.pms.common.constants.ApiConstants;
import com.eyehospital.pms.module.patient.dto.PatientDashboardResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

/**
 * REST API contract for patient operations.
 *
 * <p>All Swagger / OpenAPI annotations live here, keeping the implementation
 * class free of documentation noise (Single Responsibility principle).</p>
 *
 * <p>Base path: {@code /api/v1/patients}</p>
 */
@Tag(name = "Patient", description = "Patient management and dashboard operations")
@RequestMapping(ApiConstants.PATIENTS)
public interface PatientController {

    /**
     * Retrieves today's patient dashboard statistics for the authenticated user's hospital.
     *
     * <p>Returns counts for: total patients today, new visits, follow-up visits,
     * and completed appointments. Accessible by all roles.</p>
     *
     * @param request the HTTP request (used to extract hospitalId from JWT)
     * @return dashboard statistics
     */
    @GetMapping(ApiConstants.PATIENT_DASHBOARD_TODAY)
    @Operation(
            summary     = "Get today's patient dashboard",
            description = "Returns today's patient statistics: total appointments, new visits, follow-up visits, "
                        + "completed appointments, and total registered patients for the authenticated user's hospital."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description  = "Dashboard statistics retrieved successfully",
                    content      = @Content(schema = @Schema(implementation = PatientDashboardResponseDto.class))
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
    PatientDashboardResponseDto getTodayDashboard(HttpServletRequest request);
}
