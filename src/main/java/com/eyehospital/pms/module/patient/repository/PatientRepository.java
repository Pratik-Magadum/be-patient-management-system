package com.eyehospital.pms.module.patient.repository;

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
     * Counts all patients belonging to a specific hospital.
     */
    long countByHospitalId(UUID hospitalId);
}
