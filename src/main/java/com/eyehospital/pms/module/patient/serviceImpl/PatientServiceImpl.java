package com.eyehospital.pms.module.patient.serviceImpl;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eyehospital.pms.module.appointment.repository.AppointmentRepository;
import com.eyehospital.pms.module.patient.dto.PatientDashboardResponseDto;
import com.eyehospital.pms.module.patient.repository.PatientRepository;
import com.eyehospital.pms.module.patient.service.PatientService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link PatientService}.
 *
 * <p>Aggregates appointment counts from {@link AppointmentRepository}
 * to build the patient dashboard statistics for today.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    @Override
    @Transactional(readOnly = true)
    public PatientDashboardResponseDto getTodayDashboard(UUID hospitalId) {
        LocalDate today = LocalDate.now();
        log.debug("Building patient dashboard for hospitalId={} on date={}", hospitalId, today);

        long totalPatients = appointmentRepository
                .countByHospitalIdAndAppointmentDate(hospitalId, today);

        long newPatients = appointmentRepository
                .countByHospitalIdAndAppointmentDateAndVisitType(hospitalId, today, "NEW_VISIT");

        long followUpPatients = appointmentRepository
                .countByHospitalIdAndAppointmentDateAndVisitType(hospitalId, today, "FOLLOW_UP");

        long completedPatients = appointmentRepository
                .countByHospitalIdAndAppointmentDateAndStatus(hospitalId, today, "COMPLETED");

        long totalRegisteredPatients = patientRepository.countByHospitalId(hospitalId);

        log.debug("Dashboard stats - total:{}, new:{}, followUp:{}, completed:{}, registered:{}",
                totalPatients, newPatients, followUpPatients, completedPatients, totalRegisteredPatients);

        return PatientDashboardResponseDto.builder()
                .date(today)
                .totalPatients(totalPatients)
                .newPatients(newPatients)
                .followUpPatients(followUpPatients)
                .completedPatients(completedPatients)
                .totalRegisteredPatients(totalRegisteredPatients)
                .build();
    }
}
