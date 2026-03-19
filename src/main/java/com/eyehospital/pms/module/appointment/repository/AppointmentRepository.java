package com.eyehospital.pms.module.appointment.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
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
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

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
     * Searches appointments by optional criteria: patient name, phone number,
     * specific date, or date range. All criteria are combined with AND.
     *
     * <p>Uses a native query to work around a PostgreSQL/Hibernate type mismatch
     * (character varying ~~ bytea) that occurs when nullable parameters are used
     * in JPQL LIKE expressions.</p>
     *
     * @param hospitalId the tenant hospital identifier
     * @param name       partial patient name (case-insensitive), or null to skip
     * @param phone      partial phone number, or null to skip
     * @param date       specific appointment date, or null to skip
     * @param fromDate   start of date range (inclusive), or null to skip
     * @param toDate     end of date range (inclusive), or null to skip
     * @return matching appointments with patient data
     */
    @Query(value = """
            SELECT a.* FROM appointments a
            JOIN patients p ON p.patient_id = a.patient_id
            WHERE a.hospital_id = :hospitalId
              AND (CAST(:name AS text) IS NULL OR LOWER(p.full_name) LIKE LOWER(CONCAT('%', CAST(:name AS text), '%')))
              AND (CAST(:phone AS text) IS NULL OR p.mobile_number LIKE CONCAT('%', CAST(:phone AS text), '%'))
              AND (CAST(:date AS date) IS NULL OR a.appointment_date = CAST(:date AS date))
              AND (CAST(:fromDate AS date) IS NULL OR CAST(:toDate AS date) IS NULL
                   OR a.appointment_date BETWEEN CAST(:fromDate AS date) AND CAST(:toDate AS date))
            ORDER BY a.appointment_date DESC, a.appointment_time DESC
            """, nativeQuery = true)
    List<Appointment> searchAppointments(
            @Param("hospitalId") UUID hospitalId,
            @Param("name") String name,
            @Param("phone") String phone,
            @Param("date") LocalDate date,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
