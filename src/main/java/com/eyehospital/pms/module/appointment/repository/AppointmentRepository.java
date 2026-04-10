package com.eyehospital.pms.module.appointment.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Finds an appointment by ID and hospital (tenant isolation).
     */
    Optional<Appointment> findByAppointmentIdAndHospitalId(UUID appointmentId, UUID hospitalId);

    /**
     * Finds a non-deleted appointment by ID and hospital (tenant isolation).
     */
    Optional<Appointment> findByAppointmentIdAndHospitalIdAndDeletedFalse(UUID appointmentId, UUID hospitalId);

    /**
     * Checks whether a follow-up already exists for the given parent appointment.
     */
    boolean existsByParentAppointmentAndStatusNotAndDeletedFalse(Appointment parentAppointment, String status);

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

    /**
     * Counts all appointments for a given hospital within a date range (inclusive).
     */
    long countByHospitalIdAndAppointmentDateBetween(UUID hospitalId, LocalDate fromDate, LocalDate toDate);

    /**
     * Counts appointments by visit type for a given hospital within a date range (inclusive).
     */
    long countByHospitalIdAndAppointmentDateBetweenAndVisitType(UUID hospitalId, LocalDate fromDate, LocalDate toDate, String visitType);

    /**
     * Counts appointments by status for a given hospital within a date range (inclusive).
     */
    long countByHospitalIdAndAppointmentDateBetweenAndStatus(UUID hospitalId, LocalDate fromDate, LocalDate toDate, String status);

    /**
     * Soft-deletes all non-deleted appointments belonging to a given patient.
     */
    @Modifying
    @Query("UPDATE Appointment a SET a.deleted = true, a.deletedAt = CURRENT_TIMESTAMP "
         + "WHERE a.patient.patientId = :patientId AND a.deleted = false")
    void softDeleteByPatientId(@Param("patientId") UUID patientId);

    // -----------------------------------------------------------------------
    // Count methods excluding soft-deleted appointments
    // -----------------------------------------------------------------------

    long countByHospitalIdAndAppointmentDateAndDeletedFalse(UUID hospitalId, LocalDate appointmentDate);

    long countByHospitalIdAndAppointmentDateAndVisitTypeAndDeletedFalse(UUID hospitalId, LocalDate appointmentDate, String visitType);

    long countByHospitalIdAndAppointmentDateAndStatusAndDeletedFalse(UUID hospitalId, LocalDate appointmentDate, String status);

    long countByHospitalIdAndAppointmentDateBetweenAndDeletedFalse(UUID hospitalId, LocalDate fromDate, LocalDate toDate);

    long countByHospitalIdAndAppointmentDateBetweenAndVisitTypeAndDeletedFalse(UUID hospitalId, LocalDate fromDate, LocalDate toDate, String visitType);

    long countByHospitalIdAndAppointmentDateBetweenAndStatusAndDeletedFalse(UUID hospitalId, LocalDate fromDate, LocalDate toDate, String status);

    /**
     * Retrieves all non-deleted appointments for a patient within a hospital,
     * ordered by appointment date descending (most recent first).
     *
     * <p>The caller uses the returned entities' JPA relationships
     * ({@code doctor}, {@code consultation → prescriptions → medicine})
     * to build the patient history response.</p>
     */
    List<Appointment> findByPatientIdAndHospitalIdAndDeletedFalseOrderByAppointmentDateDescAppointmentTimeDesc(
            UUID patientId, UUID hospitalId);
}
