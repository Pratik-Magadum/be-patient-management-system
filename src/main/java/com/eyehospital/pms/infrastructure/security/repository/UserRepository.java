package com.eyehospital.pms.infrastructure.security.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eyehospital.pms.infrastructure.security.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsernameAndActiveTrue(String username);

    Optional<User> findByHospitalIdAndUsernameAndActiveTrue(UUID hospitalId, String username);
}
