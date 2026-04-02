package com.eyehospital.pms.common.mapper;

import java.util.Comparator;

import com.eyehospital.pms.module.appointment.dto.RegisterAppointmentRequestDto;
import com.eyehospital.pms.module.appointment.entity.Appointment;
import com.eyehospital.pms.module.patient.dto.PatientSearchResponseDto;
import com.eyehospital.pms.module.patient.entity.Patient;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Centralised mapping between Patient/Appointment entities,
 * request DTOs and response DTOs.
 *
 * <p>Eliminates duplicate mapping code in PatientServiceImpl
 * and AppointmentServiceImpl.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PatientMapper {

    /**
     * Copies patient-related fields from the request DTO onto the entity.
     * Used by both registration (new patient) and update flows.
     */
    public static void applyDtoToPatient(RegisterAppointmentRequestDto dto, Patient patient) {
        patient.setFullName(dto.getFullName());
        patient.setMobileNumber(dto.getMobileNumber());
        patient.setEmail(dto.getEmail());
        patient.setAge(dto.getAge());
        patient.setGender(dto.getGender());
        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setAddress(dto.getAddress());
    }

    /**
     * Maps an Appointment (with its associated Patient) to a response DTO.
     */
    public static PatientSearchResponseDto toResponseDto(Appointment appointment) {
        return buildResponseDto(appointment.getPatient(), appointment);
    }

    /**
     * Maps a Patient entity to a response DTO using its latest non-deleted appointment.
     */
    public static PatientSearchResponseDto toResponseDto(Patient patient) {
        Appointment latest = patient.getAppointments().stream()
                .filter(a -> !a.isDeleted())
                .max(Comparator.comparing(Appointment::getAppointmentDate)
                        .thenComparing(Appointment::getAppointmentTime))
                .orElse(null);
        return buildResponseDto(patient, latest);
    }

    // -----------------------------------------------------------------------
    // Internal helper
    // -----------------------------------------------------------------------

    private static PatientSearchResponseDto buildResponseDto(Patient patient, Appointment appointment) {
        return PatientSearchResponseDto.builder()
                .appointmentId(appointment != null ? appointment.getAppointmentId() : null)
                .patientId(patient.getPatientId())
                .patientNumber(patient.getPatientNumber())
                .patientName(patient.getFullName())
                .mobileNumber(patient.getMobileNumber())
                .age(patient.getAge())
                .gender(patient.getGender())
                .email(patient.getEmail())
                .dateOfBirth(patient.getDateOfBirth())
                .address(patient.getAddress())
                .visitType(appointment != null ? appointment.getVisitType() : null)
                .appointmentDate(appointment != null ? appointment.getAppointmentDate() : null)
                .appointmentTime(appointment != null ? appointment.getAppointmentTime() : null)
                .appointmentStatus(appointment != null ? appointment.getStatus() : null)
                .notes(appointment != null ? appointment.getNotes() : null)
                .build();
    }
}
