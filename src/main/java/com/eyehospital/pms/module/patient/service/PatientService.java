package com.eyehospital.pms.module.patient.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.eyehospital.pms.module.appointment.dto.RegisterAppointmentRequestDto;
import com.eyehospital.pms.module.patient.dto.PatientDashboardResponseDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchListResponseDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchRequestDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchResponseDto;

/**
 * Contract for patient operations.
 *
 * <p>Declaring behaviour as an interface (Open/Closed principle) ensures the
 * service layer can be extended or swapped without altering callers.</p>
 */
public interface PatientService {

    /**
     * Retrieves patient dashboard statistics for the given hospital.
     *
     * <p>When {@code fromDate} and {@code toDate} are both null, defaults to today.
     * When both dates are provided, computes statistics for the date range.</p>
     *
     * @param hospitalId the tenant hospital identifier
     * @param fromDate   start date (inclusive), or null for today
     * @param toDate     end date (inclusive), or null for today
     * @return dashboard statistics including total, new, follow-up, and completed patient counts
     */
    PatientDashboardResponseDto getDashboard(UUID hospitalId, LocalDate fromDate, LocalDate toDate);

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

    /**
     * Searches patients by name and/or phone number across all appointment dates.
     *
     * @param hospitalId the tenant hospital identifier
     * @param name       partial patient name (case-insensitive), nullable
     * @param phoneNumber partial phone number, nullable
     * @return list of matching patient–appointment records
     */
    List<PatientSearchResponseDto> searchByNamePhone(UUID hospitalId, String name, String phoneNumber);

    /**
     * Updates an existing patient's details.
     *
     * @param hospitalId the tenant hospital identifier
     * @param patientId  the patient to update
     * @param request    the update request containing new patient details
     * @return the updated patient details
     */
    PatientSearchResponseDto updatePatient(UUID hospitalId, UUID patientId, RegisterAppointmentRequestDto request);

    /**
     * Soft-deletes a patient and all associated appointments.
     *
     * @param hospitalId the tenant hospital identifier
     * @param patientId  the patient to soft-delete
     */
    void deletePatient(UUID hospitalId, UUID patientId);
}
