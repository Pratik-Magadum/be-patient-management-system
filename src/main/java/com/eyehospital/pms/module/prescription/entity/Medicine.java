package com.eyehospital.pms.module.prescription.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing the {@code medicines} master catalog.
 *
 * <p>Medicines are global (not tenant-scoped) and shared across all hospitals.</p>
 */
@Entity
@Table(name = "medicines")
@Getter
@Setter
@NoArgsConstructor
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "medicine_id", updatable = false, nullable = false)
    private UUID medicineId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "generic_name", length = 200)
    private String genericName;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "standard_dosage", length = 100)
    private String standardDosage;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT})
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private LocalDateTime updatedAt;
}
