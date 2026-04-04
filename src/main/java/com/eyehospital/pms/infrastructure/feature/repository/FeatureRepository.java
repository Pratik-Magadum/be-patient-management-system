package com.eyehospital.pms.infrastructure.feature.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eyehospital.pms.infrastructure.feature.entity.Feature;

/**
 * Spring Data JPA repository for {@link Feature} entities.
 */
@Repository
public interface FeatureRepository extends JpaRepository<Feature, UUID> {

    /**
     * Finds all feature flags for the given role.
     *
     * @param role the user role (e.g. {@code ADMIN}, {@code RECEPTIONIST})
     * @return list of features configured for this role
     */
    List<Feature> findByRole(String role);
}
