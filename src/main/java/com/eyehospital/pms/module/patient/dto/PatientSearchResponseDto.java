package com.eyehospital.pms.module.patient.dto;

import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO for patient search results.
 *
 * <p>Each record represents a patient–appointment pair, providing the
 * patient name, mobile number, visit type, appointment time, and status.</p>
 */
@Getter
@Builder
@Schema(description = "Patient search result with appointment details")
public class PatientSearchResponseDto {

    @Schema(description = "Patient full name", example = "Rajesh Kumar")
    private final String patientName;

    @Schema(description = "Patient mobile number", example = "+91-9800000001")
    private final String mobileNumber;

    @Schema(description = "Visit type: NEW_VISIT or FOLLOW_UP", example = "NEW_VISIT")
    private final String visitType;

    @Schema(description = "Appointment scheduled time", example = "09:30:00")
    private final LocalTime appointmentTime;

    @Schema(description = "Appointment status: REGISTERED, IN_PROGRESS, or COMPLETED", example = "REGISTERED")
    private final String appointmentStatus;
}
