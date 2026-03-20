package com.eyehospital.pms.module.patient.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO for the patient dashboard endpoint.
 *
 * <p>Aggregates today's appointment statistics for a hospital:
 * total patients, new visits, follow-ups, and completed appointments.</p>
 */
@Getter
@Builder
@Schema(description = "Patient dashboard statistics for today")
public class PatientDashboardResponseDto {

    @Schema(description = "Date for which the statistics are generated", example = "2026-03-12")
    private final LocalDate date;

    @Schema(description = "Total number of patients with appointments today", example = "10")
    private final long totalPatients;

    @Schema(description = "Number of new visit patients today", example = "6")
    private final long newPatients;

    @Schema(description = "Number of follow-up patients today", example = "4")
    private final long followUpPatients;

    @Schema(description = "Number of completed appointments today", example = "3")
    private final long completedPatients;

    @Schema(description = "Total registered patients across all time for this hospital", example = "150")
    private final long totalRegisteredPatients;
}
