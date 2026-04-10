package com.eyehospital.pms.module.prescription.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eyehospital.pms.module.prescription.entity.Prescription;

/**
 * Spring Data JPA repository for {@link Prescription} entities.
 */
@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
}
