package com.eyehospital.pms.module.patient.service;

import java.util.UUID;

import com.eyehospital.pms.module.patient.dto.PatientDashboardResponseDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchListResponseDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchRequestDto;

/**
 * Contract for patient operations.
 *
 * <p>Declaring behaviour as an interface (Open/Closed principle) ensures the
 * service layer can be extended or swapped without altering callers.</p>
 */
public interface PatientService {

    /**
     * Retrieves today's patient dashboard statistics for the given hospital.
     *
     * @param hospitalId the tenant hospital identifier
     * @return dashboard statistics including today's total, new, follow-up, and completed patient counts
     */
    PatientDashboardResponseDto getTodayDashboard(UUID hospitalId);

    /**
     * Searches patients by the criteria in the request DTO.
     *
     * <p>If the request is null or empty, returns today's patients.
     * When both {@code fromDate} and {@code toDate} are the same,
     * it fetches appointments for that single date.</p>
     *
     * @param hospitalId the tenant hospital identifier
     * @param request    search criteria (nullable – defaults to today)
     * @return search results with summary counts and matching patient–appointment records
     */
    PatientSearchListResponseDto getPatients(UUID hospitalId, PatientSearchRequestDto request);
}
