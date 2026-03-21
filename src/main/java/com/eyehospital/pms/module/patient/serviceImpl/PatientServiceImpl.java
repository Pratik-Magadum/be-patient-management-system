package com.eyehospital.pms.module.patient.serviceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eyehospital.pms.module.appointment.entity.Appointment;
import com.eyehospital.pms.module.appointment.repository.AppointmentRepository;
import com.eyehospital.pms.module.appointment.repository.AppointmentSpecification;
import com.eyehospital.pms.module.patient.dto.PatientDashboardResponseDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchListResponseDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchRequestDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchResponseDto;
import com.eyehospital.pms.module.patient.entity.Patient;
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
    public PatientSearchListResponseDto getPatients(UUID hospitalId, PatientSearchRequestDto request) {

        Specification<Appointment> spec = Specification
                .where(AppointmentSpecification.hasHospitalId(hospitalId));

        int page = 0;
        int size = 10;

        if (request != null) {
            page = request.getPage();
            size = request.getSize();
        }

        // Default to today's patients when request is null or empty
        if (request == null || request.isEmpty()) {
            LocalDate today = LocalDate.now();
            log.debug("No search criteria provided — returning today's patients for hospitalId={}", hospitalId);
            spec = spec.and(AppointmentSpecification.onDate(today));
        } else {
            LocalDate fromDate = request.getFromDate();
            LocalDate toDate = request.getToDate();

            // When both dates are equal, treat as single-date search
            if (fromDate != null && toDate != null && fromDate.isEqual(toDate)) {
                spec = spec.and(AppointmentSpecification.onDate(fromDate));
            } else if (fromDate != null && toDate != null) {
                spec = spec.and(AppointmentSpecification.betweenDates(fromDate, toDate));
            }

            log.debug("Searching patients for hospitalId={} fromDate={} toDate={} page={} size={}",
                    hospitalId, fromDate, toDate, page, size);
        }

        spec = spec.and(AppointmentSpecification.orderByStatusThenDateTimeAsc());

        Pageable pageable = PageRequest.of(page, size);

        Page<Appointment> appointmentPage = appointmentRepository.findAll(spec, pageable);

        List<PatientSearchResponseDto> patients = appointmentPage.getContent().stream()
                .map(this::toSearchResponseDto)
                .toList();

        long newPatients = appointmentPage.getContent().stream()
                .filter(a -> "NEW_VISIT".equals(a.getVisitType())).count();
        long followUpPatients = appointmentPage.getContent().stream()
                .filter(a -> "FOLLOW_UP".equals(a.getVisitType())).count();
        long completedPatients = appointmentPage.getContent().stream()
                .filter(a -> "COMPLETED".equals(a.getStatus())).count();

        return PatientSearchListResponseDto.builder()
                .totalPatients(appointmentPage.getTotalElements())
                .newPatients(newPatients)
                .followUpPatients(followUpPatients)
                .completedPatients(completedPatients)
                .currentPage(appointmentPage.getNumber())
                .pageSize(appointmentPage.getSize())
                .totalPages(appointmentPage.getTotalPages())
                .patients(patients)
                .build();
    }

    // -----------------------------------------------------------------------
    // Private mapping helpers
    // -----------------------------------------------------------------------

    private PatientSearchResponseDto toSearchResponseDto(Appointment appointment) {
        Patient patient = appointment.getPatient();
        return PatientSearchResponseDto.builder()
                .patientNumber(patient.getPatientNumber())
                .patientName(patient.getFullName())
                .mobileNumber(patient.getMobileNumber())
                .age(patient.getAge())
                .gender(patient.getGender())
                .email(patient.getEmail())
                .dateOfBirth(patient.getDateOfBirth())
                .address(patient.getAddress())
                .visitType(appointment.getVisitType())
                .appointmentDate(appointment.getAppointmentDate())
                .appointmentTime(appointment.getAppointmentTime())
                .appointmentStatus(appointment.getStatus())
                .build();
    }
}
