package com.eyehospital.pms.module.appointment.service;

import java.util.UUID;

import com.eyehospital.pms.common.enums.AppointmentStatus;
import com.eyehospital.pms.module.appointment.dto.FollowUpRequestDto;
import com.eyehospital.pms.module.appointment.dto.RegisterAppointmentRequestDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchResponseDto;

/**
 * Contract for appointment operations.
 */
public interface AppointmentService {

    /**
     * Registers a new patient and creates their first appointment.
     *
     * @param hospitalId the tenant hospital identifier
     * @param request    patient and appointment details
     * @return the newly created patient-appointment details
     */
    PatientSearchResponseDto registerAppointment(UUID hospitalId, RegisterAppointmentRequestDto request);

    /**
     * Registers a follow-up appointment linked to an existing (completed) parent appointment.
     *
     * @param hospitalId the tenant hospital identifier
     * @param request    follow-up registration details
     * @return the newly created follow-up appointment details
     */
    PatientSearchResponseDto registerFollowUp(UUID hospitalId, FollowUpRequestDto request);

    /**
     * Soft-deletes an appointment by setting is_deleted = true.
     *
     * @param hospitalId    the tenant hospital identifier
     * @param appointmentId the appointment to delete
     */
    void deleteAppointment(UUID hospitalId, UUID appointmentId);

    /**
     * Updates the status of an appointment.
     *
     * <p>Valid transitions: REGISTERED → IN_PROGRESS → COMPLETED.</p>
     *
     * @param hospitalId    the tenant hospital identifier
     * @param appointmentId the appointment to update
     * @param request       the request containing the new status
     * @return the updated appointment details
     */
    PatientSearchResponseDto updateStatus(UUID hospitalId, UUID appointmentId, AppointmentStatus status);
}
