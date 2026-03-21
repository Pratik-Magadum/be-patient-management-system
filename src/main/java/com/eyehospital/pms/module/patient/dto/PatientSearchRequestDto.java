package com.eyehospital.pms.module.patient.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for patient search.
 *
 * <p>All fields are optional. When no field is provided (or the DTO is null),
 * the API returns today's patients by default.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Patient search criteria — all fields are optional")
public class PatientSearchRequestDto {

    @Schema(description = "Start date for appointment search (inclusive, ISO format). "
            + "Must be equal to or before toDate.", example = "2026-03-15")
    private LocalDate fromDate;

    @Schema(description = "End date for appointment search (inclusive, ISO format). "
            + "Must be equal to or after fromDate.", example = "2026-03-19")
    private LocalDate toDate;

    @Builder.Default
    @Schema(description = "Page number (0-based)", example = "0")
    private int page = 0;

    @Builder.Default
    @Schema(description = "Page size", example = "10")
    private int size = 10;

    /**
     * Returns {@code true} if no meaningful search criterion has been set.
     */
    @JsonIgnore
    public boolean isEmpty() {
        return fromDate == null && toDate == null;
    }
}
