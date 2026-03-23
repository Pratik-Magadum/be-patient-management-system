package com.eyehospital.pms.module.patient.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eyehospital.pms.common.exception.BusinessException;
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
    public PatientDashboardResponseDto getDashboard(UUID hospitalId, LocalDate fromDate, LocalDate toDate) {
        // Default to today when no dates provided
        if (fromDate == null || toDate == null) {
            fromDate = LocalDate.now();
            toDate = fromDate;
        }

        log.debug("Building patient dashboard for hospitalId={} from={} to={}", hospitalId, fromDate, toDate);

        long totalPatients;
        long newPatients;
        long followUpPatients;
        long completedPatients;

        if (fromDate.isEqual(toDate)) {
            // Single-date queries (existing optimised methods)
            totalPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateAndPatientDeletedFalse(hospitalId, fromDate);
            newPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateAndVisitTypeAndPatientDeletedFalse(hospitalId, fromDate, "NEW_VISIT");
            followUpPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateAndVisitTypeAndPatientDeletedFalse(hospitalId, fromDate, "FOLLOW_UP");
            completedPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateAndStatusAndPatientDeletedFalse(hospitalId, fromDate, "COMPLETED");
        } else {
            // Date-range queries
            totalPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateBetweenAndPatientDeletedFalse(hospitalId, fromDate, toDate);
            newPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateBetweenAndVisitTypeAndPatientDeletedFalse(hospitalId, fromDate, toDate, "NEW_VISIT");
            followUpPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateBetweenAndVisitTypeAndPatientDeletedFalse(hospitalId, fromDate, toDate, "FOLLOW_UP");
            completedPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateBetweenAndStatusAndPatientDeletedFalse(hospitalId, fromDate, toDate, "COMPLETED");
        }

        long totalRegisteredPatients = patientRepository.countByHospitalIdAndDeletedFalse(hospitalId);

        return PatientDashboardResponseDto.builder()
                .date(fromDate)
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

        LocalDate fromDate = null;
        LocalDate toDate = null;
        int page = 0;
        int size = 10;

        Specification<Appointment> spec = Specification
                .where(AppointmentSpecification.hasHospitalId(hospitalId))
                .and(AppointmentSpecification.patientNotDeleted());

        if (request != null) {
            page = request.getPage();
            size = request.getSize();
        }

        // Default to today's patients when request is null or empty
        if (request == null || request.isEmpty()) {
            LocalDate today = LocalDate.now();
            fromDate = today;
            toDate = today;
            log.debug("No search criteria provided — returning today's patients for hospitalId={}", hospitalId);
            spec = spec.and(AppointmentSpecification.onDate(today));
        } else {
            fromDate = request.getFromDate();
            toDate = request.getToDate();

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

        // Use repository counts for accurate totals across all pages
        PatientDashboardResponseDto dashboard = getDashboard(hospitalId, fromDate, toDate);

        return PatientSearchListResponseDto.builder()
                .totalPatients(dashboard.getTotalPatients())
                .newPatients(dashboard.getNewPatients())
                .followUpPatients(dashboard.getFollowUpPatients())
                .completedPatients(dashboard.getCompletedPatients())
                .currentPage(appointmentPage.getNumber())
                .pageSize(appointmentPage.getSize())
                .totalPages(appointmentPage.getTotalPages())
                .patients(patients)
                .build();
    }

    // -----------------------------------------------------------------------
    // Search by name and phone
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<PatientSearchResponseDto> searchByNamePhone(UUID hospitalId, String name, String phoneNumber) {
        log.debug("Searching by name/phone for hospitalId={} name={} phoneNumber={}", hospitalId, name, phoneNumber);

        Specification<Appointment> spec = Specification
                .where(AppointmentSpecification.hasHospitalId(hospitalId))
                .and(AppointmentSpecification.patientNotDeleted());

        if (name != null && !name.isBlank()) {
            spec = spec.and(AppointmentSpecification.patientNameContains(name.trim()));
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            spec = spec.and(AppointmentSpecification.patientPhoneContains(phoneNumber.trim()));
        }

        spec = spec.and(AppointmentSpecification.orderByStatusThenDateTimeAsc());

        return appointmentRepository.findAll(spec).stream()
                .map(this::toSearchResponseDto)
                .toList();
    }

    // -----------------------------------------------------------------------
    // Soft delete
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public void softDeletePatient(UUID hospitalId, UUID patientId) {
        Patient patient = patientRepository
                .findByPatientIdAndHospitalIdAndDeletedFalse(patientId, hospitalId)
                .orElseThrow(() -> new BusinessException("PATIENT_NOT_FOUND",
                        "Patient not found, already deleted, or belongs to another hospital"));

        patient.setDeleted(true);
        patient.setDeletedAt(LocalDateTime.now());
        patientRepository.save(patient);

        log.info("Soft-deleted patient {} for hospital {}", patientId, hospitalId);
    }

    // -----------------------------------------------------------------------
    // Private mapping helpers
    // -----------------------------------------------------------------------

    private PatientSearchResponseDto toSearchResponseDto(Appointment appointment) {
        Patient patient = appointment.getPatient();
        return PatientSearchResponseDto.builder()
                .patientId(patient.getPatientId())
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
