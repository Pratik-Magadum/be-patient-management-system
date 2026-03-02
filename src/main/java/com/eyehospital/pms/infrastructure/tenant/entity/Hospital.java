package com.eyehospital.pms.infrastructure.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing the {@code hospitals} table.
 *
 * <p>Each hospital is an independent tenant in the system. All other domain
 * entities (patients, appointments, etc.) reference this table via
 * {@code hospital_id}.</p>
 *
 * <ul>
 *   <li>{@code createdAt} is set by the DB default ({@code NOW()}) and never
 *       written by the application.</li>
 *   <li>{@code updatedAt} is maintained by the {@code set_updated_at_hospitals}
 *       trigger and refreshed after every UPDATE.</li>
 * </ul>
 */
@Entity
@Table(name = "hospitals")
@Getter
@Setter
@NoArgsConstructor
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hospital_id", updatable = false, nullable = false)
    private UUID hospitalId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * Unique subdomain slug used for tenant resolution (e.g. {@code apollo-eye}).
     */
    @Column(name = "subdomain", unique = true, nullable = false, length = 50)
    private String subdomain;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "contact_email", nullable = false, length = 50)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT})
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private LocalDateTime updatedAt;
}
