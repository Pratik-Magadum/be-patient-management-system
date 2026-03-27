package com.eyehospital.pms.module.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to register a follow-up appointment")
public class FollowUpRequestDto {

    @NotNull(message = "parentAppointmentId is required")
    @Schema(description = "UUID of the parent appointment being followed up", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID parentAppointmentId;

    @NotNull(message = "appointmentDate is required")
    @Schema(description = "Date for the follow-up appointment (ISO format)", example = "2026-04-01")
    private LocalDate appointmentDate;

    @NotNull(message = "appointmentTime is required")
    @Schema(description = "Time for the follow-up appointment", example = "10:30:00")
    private LocalTime appointmentTime;

    @Schema(description = "Optional notes for the follow-up", example = "Post-surgery checkup")
    private String notes;
}
