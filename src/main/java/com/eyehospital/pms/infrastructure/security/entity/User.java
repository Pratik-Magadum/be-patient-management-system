package com.eyehospital.pms.infrastructure.security.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps to the existing `users` table created by Liquibase DDL.
 * Roles: RECEPTIONIST, ASSISTANT, DOCTOR, ADMIN
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uq_users_hospital_username", columnNames = {"hospital_id", "username"})
})
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}