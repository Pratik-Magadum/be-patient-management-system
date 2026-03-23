package com.eyehospital.pms.module.patient.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO for patient search results.
 *
 * <p>Each record represents a patient–appointment pair with full patient details.</p>
 */
@Getter
@Builder
@JsonPropertyOrder({"patientId", "patientNumber", "patientName", "mobileNumber", "age", "gender",
        "email", "dateOfBirth", "address", "visitType", "appointmentDate",
        "appointmentTime", "appointmentStatus"})
@Schema(description = "Patient search result with full patient and appointment details")
public class PatientSearchResponseDto {

    @Schema(description = "Patient unique identifier", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private final UUID patientId;

    @Schema(description = "Patient number", example = "PT-2026-00001")
    private final String patientNumber;

    @Schema(description = "Patient full name", example = "Rajesh Kumar")
    private final String patientName;

    @Schema(description = "Patient mobile number", example = "+91-9800000001")
    private final String mobileNumber;

    @Schema(description = "Patient age", example = "35")
    private final Integer age;

    @Schema(description = "Patient gender: MALE, FEMALE, or OTHER", example = "MALE")
    private final String gender;

    @Schema(description = "Patient email", example = "rajesh@email.com")
    private final String email;

    @Schema(description = "Patient date of birth", example = "1990-05-15")
    private final LocalDate dateOfBirth;

    @Schema(description = "Patient address", example = "12 MG Road, Mumbai")
    private final String address;

    @Schema(description = "Visit type: NEW_VISIT or FOLLOW_UP", example = "NEW_VISIT")
    private final String visitType;

    @Schema(description = "Appointment date", example = "2026-03-20")
    private final LocalDate appointmentDate;

    @Schema(description = "Appointment scheduled time", example = "09:30:00")
    private final LocalTime appointmentTime;

    @Schema(description = "Appointment status: REGISTERED, IN_PROGRESS, or COMPLETED", example = "REGISTERED")
    private final String appointmentStatus;
}
