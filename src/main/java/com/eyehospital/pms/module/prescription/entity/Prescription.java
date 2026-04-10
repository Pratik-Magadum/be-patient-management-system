package com.eyehospital.pms.module.prescription.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.eyehospital.pms.module.consultation.entity.Consultation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing the {@code prescriptions} table.
 *
 * <p>Each prescription links a consultation to a medicine with
 * dosage, frequency, and duration details.</p>
 */
@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "prescription_id", updatable = false, nullable = false)
    private UUID prescriptionId;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(name = "consultation_id", nullable = false, insertable = false, updatable = false)
    private UUID consultationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;

    @Column(name = "medicine_id", nullable = false, insertable = false, updatable = false)
    private UUID medicineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(name = "dosage", length = 100)
    private String dosage;

    @Column(name = "frequency", length = 100)
    private String frequency;

    @Column(name = "duration", length = 100)
    private String duration;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT})
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private LocalDateTime updatedAt;
}
