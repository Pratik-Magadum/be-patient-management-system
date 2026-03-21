package com.eyehospital.pms.module.patient.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.eyehospital.pms.common.constants.ApiConstants;
import com.eyehospital.pms.module.patient.dto.PatientDashboardResponseDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchListResponseDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchRequestDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
            summary     = "Get patient dashboard",
            description = "Returns patient statistics: total appointments, new visits, follow-up visits, "
                        + "completed appointments, and total registered patients for the authenticated user's hospital. "
                        + "Defaults to today when no dates are provided. "
                        + "Both fromDate and toDate must be provided together for date range queries."
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
                    responseCode = "409",
                    description  = "Invalid date parameters",
                    content      = @Content(schema = @Schema(hidden = true))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description  = "Unexpected server error",
                    content      = @Content(schema = @Schema(hidden = true))
            )
    })
    PatientDashboardResponseDto getDashboard(
            @Parameter(description = "Start date (inclusive, ISO format)", example = "2026-03-15")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "End date (inclusive, ISO format)", example = "2026-03-21")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            HttpServletRequest request);

    /**
     * Searches patients by criteria in the request DTO.
     *
     * <p>If no criteria are provided (null or empty DTO), returns today's patients.
     * When {@code fromDate} equals {@code toDate}, fetches for that single date.
     * {@code fromDate} must be equal to or before {@code toDate}.</p>
     *
     * @param searchRequest search criteria (all fields optional)
     * @param request       the HTTP request (used to extract hospitalId from JWT)
     * @return list of matching patient–appointment records (empty list if none found)
     */
    @GetMapping(ApiConstants.PATIENT_SEARCH)
    @Operation(
            summary     = "Get patients (paginated)",
            description = "Searches patients by date range with pagination. "
                        + "If no criteria are provided, returns today's patients. "
                        + "fromDate must be equal to or before toDate. "
                        + "Returns full patient details with appointment information."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description  = "Patients retrieved successfully",
                    content      = @Content(schema = @Schema(implementation = PatientSearchListResponseDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description  = "Invalid search parameters (e.g. fromDate after toDate, or only one date provided)",
                    content      = @Content(schema = @Schema(hidden = true))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description  = "Unauthorized — missing or invalid JWT token",
                    content      = @Content(schema = @Schema(hidden = true))
            )
    })
    PatientSearchListResponseDto getPatients(
            PatientSearchRequestDto searchRequest,
            HttpServletRequest request);
}
