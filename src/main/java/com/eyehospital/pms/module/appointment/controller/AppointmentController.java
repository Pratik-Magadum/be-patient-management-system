package com.eyehospital.pms.module.appointment.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.eyehospital.pms.common.constants.ApiConstants;
import com.eyehospital.pms.module.appointment.dto.FollowUpRequestDto;
import com.eyehospital.pms.module.appointment.dto.RegisterAppointmentRequestDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * REST API contract for appointment operations.
 *
 * <p>Base path: {@code /api/v1/appointments}</p>
 */
@Tag(name = "Appointment", description = "Appointment management operations")
@RequestMapping(ApiConstants.APPOINTMENTS)
public interface AppointmentController {

    @PostMapping(ApiConstants.APPOINTMENT_REGISTER)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary     = "Register a new patient appointment",
            description = "Creates a new patient record and their first appointment (NEW_VISIT). "
                        + "Required fields: fullName, mobileNumber, appointmentDate, appointmentTime."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description  = "Patient and appointment registered successfully",
                    content      = @Content(schema = @Schema(implementation = PatientSearchResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description  = "Missing or invalid required fields",
                    content      = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description  = "Unauthorized \u2014 missing or invalid JWT token",
                    content      = @Content(schema = @Schema(hidden = true))
            )
    })
    PatientSearchResponseDto registerAppointment(
            @Valid @RequestBody RegisterAppointmentRequestDto request,
            HttpServletRequest httpRequest);

    @PostMapping(ApiConstants.APPOINTMENT_FOLLOW_UP)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary     = "Register a follow-up appointment",
            description = "Creates a follow-up appointment linked to a completed parent appointment. "
                        + "The parent must be COMPLETED and the patient must not be soft-deleted."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description  = "Follow-up appointment registered successfully",
                    content      = @Content(schema = @Schema(implementation = PatientSearchResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description  = "Missing or invalid fields (parentAppointmentId, appointmentDate, appointmentTime)",
                    content      = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description  = "Parent not found, not completed, wrong tenant, or patient deleted",
                    content      = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description  = "Unauthorized — missing or invalid JWT token",
                    content      = @Content(schema = @Schema(hidden = true))
            )
    })
    PatientSearchResponseDto registerFollowUp(
            @Valid @RequestBody FollowUpRequestDto request,
            HttpServletRequest httpRequest);

    @DeleteMapping(ApiConstants.APPOINTMENT_DELETE)
    @Operation(
            summary     = "Soft-delete an appointment",
            description = "Marks an appointment as deleted (is_deleted=true) without physically removing the record. "
                        + "Deleted appointments are excluded from all search and dashboard results."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description  = "Appointment soft-deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description  = "Appointment not found, belongs to another hospital, or already deleted",
                    content      = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description  = "Unauthorized \u2014 missing or invalid JWT token",
                    content      = @Content(schema = @Schema(hidden = true))
            )
    })
    void deleteAppointment(
            @Parameter(description = "Appointment UUID", required = true)
            @PathVariable UUID appointmentId,
            HttpServletRequest httpRequest);
}
