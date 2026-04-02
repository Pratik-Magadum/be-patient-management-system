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
import com.eyehospital.pms.common.mapper.PatientMapper;
import com.eyehospital.pms.module.appointment.dto.RegisterAppointmentRequestDto;
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
            // Single-date queries
            totalPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateAndDeletedFalse(hospitalId, fromDate);
            newPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateAndVisitTypeAndDeletedFalse(hospitalId, fromDate, "NEW_VISIT");
            followUpPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateAndVisitTypeAndDeletedFalse(hospitalId, fromDate, "FOLLOW_UP");
            completedPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateAndStatusAndDeletedFalse(hospitalId, fromDate, "COMPLETED");
        } else {
            // Date-range queries
            totalPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateBetweenAndDeletedFalse(hospitalId, fromDate, toDate);
            newPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateBetweenAndVisitTypeAndDeletedFalse(hospitalId, fromDate, toDate, "NEW_VISIT");
            followUpPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateBetweenAndVisitTypeAndDeletedFalse(hospitalId, fromDate, toDate, "FOLLOW_UP");
            completedPatients = appointmentRepository
                    .countByHospitalIdAndAppointmentDateBetweenAndStatusAndDeletedFalse(hospitalId, fromDate, toDate, "COMPLETED");
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
                .and(AppointmentSpecification.appointmentNotDeleted());

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

        // Optional status and visit-type filters
        if (request != null && request.getPatientStatus() != null && !request.getPatientStatus().isBlank()) {
            spec = spec.and(AppointmentSpecification.hasStatus(request.getPatientStatus().trim()));
        }
        if (request != null && request.getVisitType() != null && !request.getVisitType().isBlank()) {
            spec = spec.and(AppointmentSpecification.hasVisitType(request.getVisitType().trim()));
        }

        spec = spec.and(AppointmentSpecification.orderByStatusThenDateTimeAsc());

        Pageable pageable = PageRequest.of(page, size);

        Page<Appointment> appointmentPage = appointmentRepository.findAll(spec, pageable);

        List<PatientSearchResponseDto> patients = appointmentPage.getContent().stream()
                .map(PatientMapper::toResponseDto)
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
                .and(AppointmentSpecification.appointmentNotDeleted());

        if (name != null && !name.isBlank()) {
            spec = spec.and(AppointmentSpecification.patientNameContains(name.trim()));
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            spec = spec.and(AppointmentSpecification.patientPhoneContains(phoneNumber.trim()));
        }

        spec = spec.and(AppointmentSpecification.orderByStatusThenDateTimeAsc());

        return appointmentRepository.findAll(spec).stream()
                .map(PatientMapper::toResponseDto)
                .toList();
    }

    // -----------------------------------------------------------------------
    // Update patient details
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public PatientSearchResponseDto updatePatient(UUID hospitalId, UUID patientId, RegisterAppointmentRequestDto request) {
        // 1. Lookup the patient — must exist, belong to this hospital, and not be deleted
        Patient patient = patientRepository.findByPatientIdAndHospitalIdAndDeletedFalse(patientId, hospitalId)
                .orElseThrow(() -> new BusinessException("PATIENT_NOT_FOUND",
                        "Patient not found or already deleted"));

        // 2. Check for duplicate mobile number — excludes the current patient from the check
        if (patientRepository.existsByHospitalIdAndMobileNumberAndPatientIdNotAndDeletedFalse(
                hospitalId, request.getMobileNumber(), patientId)) {
            throw new BusinessException("DUPLICATE_MOBILE_NUMBER",
                    "A patient with this mobile number already exists in this hospital");
        }

        // 3. Update the patient fields
        PatientMapper.applyDtoToPatient(request, patient);

        patient = patientRepository.saveAndFlush(patient);

        log.info("Updated patient {} for hospital {}", patientId, hospitalId);

        // 4. Return the updated patient details with the latest appointment (if any)
        return PatientMapper.toResponseDto(patient);
    }

    // -----------------------------------------------------------------------
    // Soft-delete patient and appointments
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public void deletePatient(UUID hospitalId, UUID patientId) {
        Patient patient = patientRepository.findByPatientIdAndHospitalIdAndDeletedFalse(patientId, hospitalId)
                .orElseThrow(() -> new BusinessException("PATIENT_NOT_FOUND",
                        "Patient not found or already deleted"));

        // Soft-delete all non-deleted appointments for this patient
        appointmentRepository.softDeleteByPatientId(patientId);

        // Soft-delete the patient
        patient.setDeleted(true);
        patient.setDeletedAt(LocalDateTime.now());
        patientRepository.save(patient);

        log.info("Soft-deleted patient {} and appointments for hospitalId={}", patientId, hospitalId);
    }


}
