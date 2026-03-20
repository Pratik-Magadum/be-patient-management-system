package com.eyehospital.pms.module.patient.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Patient search results with summary counts")
public class PatientSearchListResponseDto {

    @Schema(description = "Total number of patients in the result set", example = "10")
    private final long totalPatients;

    @Schema(description = "Number of new visit patients", example = "6")
    private final long newPatients;

    @Schema(description = "Number of follow-up patients", example = "4")
    private final long followUpPatients;

    @Schema(description = "Number of completed appointments", example = "3")
    private final long completedPatients;

    @Schema(description = "List of patient search results")
    private final List<PatientSearchResponseDto> patients;
}
