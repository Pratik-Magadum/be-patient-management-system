package com.eyehospital.pms.module.consultation.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eyehospital.pms.module.consultation.entity.Consultation;

/**
 * Spring Data JPA repository for {@link Consultation} entities.
 */
@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {
}
