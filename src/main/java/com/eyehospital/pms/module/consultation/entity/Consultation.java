package com.eyehospital.pms.module.consultation.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.eyehospital.pms.module.appointment.entity.Appointment;
import com.eyehospital.pms.module.prescription.entity.Prescription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing the {@code consultations} table.
 *
 * <p>Each consultation is linked one-to-one with an appointment and
 * may have zero or more prescriptions.</p>
 */
@Entity
@Table(name = "consultations")
@Getter
@Setter
@NoArgsConstructor
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "consultation_id", updatable = false, nullable = false)
    private UUID consultationId;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(name = "appointment_id", nullable = false, insertable = false, updatable = false)
    private UUID appointmentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "diagnosis_notes", columnDefinition = "TEXT")
    private String diagnosisNotes;

    @Column(name = "specs_sph_right", precision = 5, scale = 2)
    private BigDecimal specsSphRight;

    @Column(name = "specs_sph_left", precision = 5, scale = 2)
    private BigDecimal specsSphLeft;

    @Column(name = "specs_cyl_right", precision = 5, scale = 2)
    private BigDecimal specsCylRight;

    @Column(name = "specs_cyl_left", precision = 5, scale = 2)
    private BigDecimal specsCylLeft;

    @Column(name = "specs_axis_right")
    private Integer specsAxisRight;

    @Column(name = "specs_axis_left")
    private Integer specsAxisLeft;

    @Column(name = "pupillary_distance", precision = 5, scale = 2)
    private BigDecimal pupillaryDistance;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @OneToMany(mappedBy = "consultation", fetch = FetchType.LAZY)
    private List<Prescription> prescriptions = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT})
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private LocalDateTime updatedAt;
}
