package com.eyehospital.pms.infrastructure.tenant.repository;

import com.eyehospital.pms.infrastructure.tenant.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Hospital} entities.
 *
 * <p>Custom finders are declared here as method-name queries, keeping all
 * data-access intent visible in one place (Single Responsibility principle).</p>
 */
@Repository
public interface HospitalRepository extends JpaRepository<Hospital, UUID> {

    /**
     * Looks up an active hospital by its unique subdomain slug.
     *
     * @param subdomain the tenant subdomain (e.g. {@code apollo-eye})
     * @return the matching hospital if it exists and is active
     */
    Optional<Hospital> findBySubdomainAndActiveTrue(String subdomain);
}
