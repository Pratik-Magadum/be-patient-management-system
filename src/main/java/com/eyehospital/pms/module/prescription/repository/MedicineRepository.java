package com.eyehospital.pms.module.prescription.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eyehospital.pms.module.prescription.entity.Medicine;

/**
 * Spring Data JPA repository for {@link Medicine} entities.
 */
@Repository
public interface MedicineRepository extends JpaRepository<Medicine, UUID> {
}
