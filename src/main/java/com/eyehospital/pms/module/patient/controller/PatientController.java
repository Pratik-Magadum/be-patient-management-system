package com.eyehospital.pms.module.patient.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.eyehospital.pms.common.constants.ApiConstants;
import com.eyehospital.pms.module.patient.dto.PatientDashboardResponseDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchListResponseDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchRequestDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchResponseDto;

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
    @GetMapping(ApiConstants.PATIENT_BY_DATES)
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

    /**
     * Searches patients by name and/or phone number across all appointment dates.
     *
     * <p>At least one of {@code name} or {@code phone} must be provided.
     * Both are partial-match: name is case-insensitive, phone matches any substring.</p>
     *
     * @param name    partial patient name (case-insensitive), optional
     * @param phoneNumber   partial phone number, optional
     * @param request the HTTP request (used to extract hospitalId from JWT)
     * @return list of matching patient–appointment records
     */
    @GetMapping(ApiConstants.PATIENT_SEARCH_BY_NAME_PHONE)
    @Operation(
            summary     = "Search patients by name and/or phone number",
            description = "Searches patients by partial name (case-insensitive) and/or partial phone number. "
                        + "At least one of name or phone must be provided. "
                        + "Returns all matching appointments across all dates."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description  = "Matching patients retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description  = "Neither name nor phone provided",
                    content      = @Content(schema = @Schema(hidden = true))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description  = "Unauthorized — missing or invalid JWT token",
                    content      = @Content(schema = @Schema(hidden = true))
            )
    })
    List<PatientSearchResponseDto> searchByNamePhone(
            @Parameter(description = "Partial patient name (case-insensitive)", example = "Rajesh")
            @RequestParam(required = false) String name,
            @Parameter(description = "Partial phone number", example = "+91-9800")
            @RequestParam(required = false) String phoneNumber,
            HttpServletRequest request);

    /**
     * Soft-deletes a patient by ID.
     *
     * @param patientId the patient UUID
     * @param request   the HTTP request (used to extract hospitalId from JWT)
     */
    @DeleteMapping(ApiConstants.PATIENT_DELETE)
    @Operation(
            summary     = "Soft-delete a patient",
            description = "Marks a patient as deleted (is_deleted=true) without physically removing the record. "
                        + "Deleted patients are excluded from all search and dashboard results."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description  = "Patient soft-deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description  = "Patient not found, belongs to another hospital, or already deleted",
                    content      = @Content(schema = @Schema(hidden = true))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description  = "Unauthorized — missing or invalid JWT token",
                    content      = @Content(schema = @Schema(hidden = true))
            )
    })
    void deletePatient(
            @Parameter(description = "Patient UUID", required = true)
            @PathVariable UUID patientId,
            HttpServletRequest request);
}
