package com.eyehospital.pms.module.patient.service;

import java.util.UUID;

import com.eyehospital.pms.module.patient.dto.PatientDashboardResponseDto;

/**
 * Contract for patient dashboard operations.
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
}
