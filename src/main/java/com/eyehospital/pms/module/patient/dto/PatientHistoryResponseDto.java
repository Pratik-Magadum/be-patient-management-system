package com.eyehospital.pms.module.patient.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO representing a single entry in a patient's visit history.
 *
 * <p>Each record corresponds to a completed appointment with its associated
 * consultation diagnosis and prescribed medicines (if any).</p>
 */
@Getter
@Builder
@JsonPropertyOrder({"appointmentId", "date", "time", "visitType", "status",
        "doctorName", "diagnosis", "medicines", "followUpDate", "notes"})
@Schema(description = "Single patient history entry")
public class PatientHistoryResponseDto {

    @Schema(description = "Appointment unique identifier",
            example = "f1e2d3c4-b5a6-7890-abcd-ef1234567890")
    private final UUID appointmentId;

    @Schema(description = "Appointment date", example = "2024-12-20")
    private final LocalDate date;

    @Schema(description = "Appointment time", example = "09:30:00")
    private final LocalTime time;

    @Schema(description = "Visit type: NEW_VISIT or FOLLOW_UP", example = "NEW_VISIT")
    private final String visitType;

    @Schema(description = "Appointment status: REGISTERED, IN_PROGRESS, or COMPLETED",
            example = "COMPLETED")
    private final String status;

    @Schema(description = "Attending doctor's name", example = "Dr. Suresh Kumar")
    private final String doctorName;

    @Schema(description = "Diagnosis notes from the consultation",
            example = "Diabetic retinopathy screening - Normal")
    private final String diagnosis;

    @Schema(description = "Prescribed medicines (comma-separated)",
            example = "Timolol Eye Drops, Artificial Tears")
    private final String medicines;

    @Schema(description = "Recommended follow-up date", example = "2025-01-20")
    private final LocalDate followUpDate;

    @Schema(description = "Appointment notes", example = "Routine eye checkup")
    private final String notes;
}
