package com.eyehospital.pms.module.patient.serviceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eyehospital.pms.module.appointment.entity.Appointment;
import com.eyehospital.pms.module.appointment.repository.AppointmentRepository;
import com.eyehospital.pms.module.appointment.repository.AppointmentSpecification;
import com.eyehospital.pms.module.patient.dto.PatientDashboardResponseDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchRequestDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchResponseDto;
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

        return PatientDashboardResponseDto.builder()
                .date(today)
                .totalPatients(totalPatients)
                .newPatients(newPatients)
                .followUpPatients(followUpPatients)
                .completedPatients(completedPatients)
                .totalRegisteredPatients(totalRegisteredPatients)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientSearchResponseDto> getPatients(UUID hospitalId, PatientSearchRequestDto request) {

        Specification<Appointment> spec = Specification
                .where(AppointmentSpecification.hasHospitalId(hospitalId));

        // Default to today's patients when request is null or empty
        if (request == null || request.isEmpty()) {
            LocalDate today = LocalDate.now();
            log.debug("No search criteria provided — returning today's patients for hospitalId={}", hospitalId);
            spec = spec.and(AppointmentSpecification.onDate(today));
        } else {
            String name = (request.getName() != null && !request.getName().isBlank())
                    ? request.getName().trim() : null;
            String phone = (request.getPhone() != null && !request.getPhone().isBlank())
                    ? request.getPhone().trim() : null;
            LocalDate fromDate = request.getFromDate();
            LocalDate toDate = request.getToDate();

            if (name != null) {
                spec = spec.and(AppointmentSpecification.patientNameContains(name));
            }
            if (phone != null) {
                spec = spec.and(AppointmentSpecification.patientPhoneContains(phone));
            }

            // When both dates are equal, treat as single-date search
            if (fromDate != null && toDate != null && fromDate.isEqual(toDate)) {
                spec = spec.and(AppointmentSpecification.onDate(fromDate));
            } else if (fromDate != null && toDate != null) {
                spec = spec.and(AppointmentSpecification.betweenDates(fromDate, toDate));
            }

            log.debug("Searching patients for hospitalId={} name={} phone={} fromDate={} toDate={}",
                    hospitalId, name, phone, fromDate, toDate);
        }

        spec = spec.and(AppointmentSpecification.orderByDateAndTimeDesc());

        List<Appointment> appointments = appointmentRepository.findAll(spec);

        return appointments.stream()
                .map(this::toSearchResponseDto)
                .toList();
    }

    // -----------------------------------------------------------------------
    // Private mapping helpers
    // -----------------------------------------------------------------------

    private PatientSearchResponseDto toSearchResponseDto(Appointment appointment) {
        return PatientSearchResponseDto.builder()
                .patientName(appointment.getPatient().getFullName())
                .mobileNumber(appointment.getPatient().getMobileNumber())
                .visitType(appointment.getVisitType())
                .appointmentTime(appointment.getAppointmentTime())
                .appointmentStatus(appointment.getStatus())
                .build();
    }
}
