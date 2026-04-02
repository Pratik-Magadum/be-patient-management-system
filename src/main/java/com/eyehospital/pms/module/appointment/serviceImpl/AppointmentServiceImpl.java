package com.eyehospital.pms.module.appointment.serviceImpl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eyehospital.pms.common.exception.BusinessException;
import com.eyehospital.pms.common.mapper.PatientMapper;
import com.eyehospital.pms.infrastructure.tenant.repository.HospitalRepository;
import com.eyehospital.pms.module.appointment.dto.FollowUpRequestDto;
import com.eyehospital.pms.module.appointment.dto.RegisterAppointmentRequestDto;
import com.eyehospital.pms.module.appointment.entity.Appointment;
import com.eyehospital.pms.module.appointment.repository.AppointmentRepository;
import com.eyehospital.pms.module.appointment.service.AppointmentService;
import com.eyehospital.pms.module.patient.dto.PatientSearchResponseDto;
import com.eyehospital.pms.module.patient.entity.Patient;
import com.eyehospital.pms.module.patient.repository.PatientRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;

    @Override
    @Transactional
    public PatientSearchResponseDto registerAppointment(UUID hospitalId, RegisterAppointmentRequestDto request) {
        // 0. Check for duplicate mobile number within the hospital
        if (patientRepository.existsByHospitalIdAndMobileNumberAndDeletedFalse(hospitalId, request.getMobileNumber())) {
            throw new BusinessException("DUPLICATE_MOBILE_NUMBER",
                    "A patient with this mobile number already exists in this hospital");
        }

        // 1. Create the patient
        Patient patient = new Patient();
        patient.setHospital(hospitalRepository.getReferenceById(hospitalId));
        PatientMapper.applyDtoToPatient(request, patient);
        patient = patientRepository.saveAndFlush(patient);

        // 2. Create the appointment
        Appointment appointment = new Appointment();
        appointment.setHospitalId(hospitalId);
        appointment.setPatient(patient);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setVisitType("NEW_VISIT");
        appointment.setStatus("REGISTERED");
        appointment.setNotes(request.getNotes());
        appointment = appointmentRepository.saveAndFlush(appointment);

        log.info("Registered patient {} with appointment {} for hospital {}",
                patient.getPatientId(), appointment.getAppointmentId(), hospitalId);

        return PatientMapper.toResponseDto(appointment);
    }

    @Override
    @Transactional
    public PatientSearchResponseDto registerFollowUp(UUID hospitalId, FollowUpRequestDto request) {
        // 1. Lookup and validate parent appointment
        Appointment parent = appointmentRepository
                .findByAppointmentIdAndHospitalId(request.getParentAppointmentId(), hospitalId)
                .orElseThrow(() -> new BusinessException("PARENT_APPOINTMENT_NOT_FOUND",
                        "Parent appointment not found or belongs to another hospital"));

        // 2. Parent must be COMPLETED
        if (!"COMPLETED".equals(parent.getStatus())) {
            throw new BusinessException("PARENT_NOT_COMPLETED",
                    "Follow-up can only be created for a completed appointment");
        }

        // 3. Patient must not be soft-deleted
        Patient patient = parent.getPatient();
        if (patient.isDeleted()) {
            throw new BusinessException("PATIENT_DELETED",
                    "Cannot create follow-up for a deleted patient");
        }

        // 4. Check if a follow-up already exists for this parent (not completed)
        if (appointmentRepository.existsByParentAppointmentAndStatusNotAndDeletedFalse(parent, "COMPLETED")) {
            throw new BusinessException("FOLLOW_UP_ALREADY_EXISTS",
                    "A follow-up appointment already exists for this parent appointment");
        }

        // 5. Create follow-up appointment
        Appointment followUp = new Appointment();
        followUp.setHospitalId(hospitalId);
        followUp.setPatient(patient);
        followUp.setAppointmentDate(request.getAppointmentDate());
        followUp.setAppointmentTime(request.getAppointmentTime());
        followUp.setVisitType("FOLLOW_UP");
        followUp.setStatus("REGISTERED");
        followUp.setParentAppointment(parent);
        followUp.setNotes(request.getNotes());

        followUp = appointmentRepository.saveAndFlush(followUp);

        log.info("Created follow-up appointment {} for parent {} in hospital {}",
                followUp.getAppointmentId(), parent.getAppointmentId(), hospitalId);

        return PatientMapper.toResponseDto(followUp);
    }

    @Override
    @Transactional
    public void deleteAppointment(UUID hospitalId, UUID appointmentId) {
        Appointment appointment = appointmentRepository
                .findByAppointmentIdAndHospitalIdAndDeletedFalse(appointmentId, hospitalId)
                .orElseThrow(() -> new BusinessException("APPOINTMENT_NOT_FOUND",
                        "Appointment not found, already deleted, or belongs to another hospital"));

        appointment.setDeleted(true);
        appointment.setDeletedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        log.info("Soft-deleted appointment {} for hospital {}", appointmentId, hospitalId);
    }

}
