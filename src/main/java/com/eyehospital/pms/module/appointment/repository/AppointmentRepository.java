package com.eyehospital.pms.module.appointment.repository;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.eyehospital.pms.module.appointment.entity.Appointment;

/**
 * Spring Data JPA repository for {@link Appointment} entities.
 *
 * <p>Provides count queries used by the patient dashboard to retrieve
 * today's appointment statistics per hospital.</p>
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {

    /**
     * Counts all appointments for a given hospital on a specific date.
     */
    long countByHospitalIdAndAppointmentDate(UUID hospitalId, LocalDate appointmentDate);

    /**
     * Counts appointments by visit type for a given hospital on a specific date.
     */
    long countByHospitalIdAndAppointmentDateAndVisitType(UUID hospitalId, LocalDate appointmentDate, String visitType);

    /**
     * Counts appointments by status for a given hospital on a specific date.
     */
    long countByHospitalIdAndAppointmentDateAndStatus(UUID hospitalId, LocalDate appointmentDate, String status);
}
