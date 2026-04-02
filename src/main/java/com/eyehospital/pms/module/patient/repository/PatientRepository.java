package com.eyehospital.pms.module.patient.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eyehospital.pms.module.patient.entity.Patient;

/**
 * Spring Data JPA repository for {@link Patient} entities.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    /**
     * Counts all non-deleted patients belonging to a specific hospital.
     */
    long countByHospitalIdAndDeletedFalse(UUID hospitalId);

    /**
     * Counts all patients belonging to a specific hospital (including deleted).
     */
    long countByHospitalId(UUID hospitalId);

    /**
     * Finds a non-deleted patient by ID and hospital.
     */
    Optional<Patient> findByPatientIdAndHospitalIdAndDeletedFalse(UUID patientId, UUID hospitalId);

    /**
     * Checks if a non-deleted patient with the given mobile number exists in the hospital.
     */
    boolean existsByHospitalIdAndMobileNumberAndDeletedFalse(UUID hospitalId, String mobileNumber);

    /**
     * Checks if a non-deleted patient with the given mobile number exists in the hospital,
     * excluding a specific patient (used during update to allow the same patient to keep their number).
     */
    boolean existsByHospitalIdAndMobileNumberAndPatientIdNotAndDeletedFalse(
            UUID hospitalId, String mobileNumber, UUID patientId);
}
