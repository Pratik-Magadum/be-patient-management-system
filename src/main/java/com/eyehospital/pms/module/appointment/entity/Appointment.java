package com.eyehospital.pms.module.appointment.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.eyehospital.pms.infrastructure.security.entity.User;
import com.eyehospital.pms.module.patient.entity.Patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing the {@code appointments} table.
 *
 * <p>Tracks patient visits with status progression:
 * {@code REGISTERED → IN_PROGRESS → COMPLETED}.</p>
 *
 * <p>Visit types: {@code NEW_VISIT} (fresh case) or {@code FOLLOW_UP}
 * (returning for a prior appointment).</p>
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "appointment_id", updatable = false, nullable = false)
    private UUID appointmentId;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(name = "patient_id", nullable = false, insertable = false, updatable = false)
    private UUID patientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "doctor_id", insertable = false, updatable = false)
    private UUID doctorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User createdUser;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "appointment_time", nullable = false)
    private LocalTime appointmentTime;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "visit_type", nullable = false, length = 20)
    private String visitType;

    @Column(name = "parent_appointment_id", insertable = false, updatable = false)
    private UUID parentAppointmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_appointment_id")
    private Appointment parentAppointment;

    @OneToMany(mappedBy = "parentAppointment")
    private List<Appointment> followUpAppointments = new ArrayList<>();

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT})
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private LocalDateTime updatedAt;
}
