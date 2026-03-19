package com.eyehospital.pms.module.patient.dto;

import java.time.LocalDate;

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
@Schema(description = "Patient search criteria — all fields are optional")
public class PatientSearchRequestDto {

    @Schema(description = "Partial patient name (case-insensitive)", example = "Rajesh")
    private String name;

    @Schema(description = "Partial phone number", example = "9800000001")
    private String phone;

    @Schema(description = "Start date for appointment search (inclusive, ISO format). "
            + "Must be equal to or before toDate.", example = "2026-03-15")
    private LocalDate fromDate;

    @Schema(description = "End date for appointment search (inclusive, ISO format). "
            + "Must be equal to or after fromDate.", example = "2026-03-19")
    private LocalDate toDate;

    /**
     * Returns {@code true} if no meaningful search criterion has been set.
     */
    public boolean isEmpty() {
        return (name == null || name.isBlank())
                && (phone == null || phone.isBlank())
                && fromDate == null
                && toDate == null;
    }
}
