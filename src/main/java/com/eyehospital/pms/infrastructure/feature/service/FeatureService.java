package com.eyehospital.pms.infrastructure.feature.service;

import java.util.Map;

/**
 * Contract for feature-flag operations.
 *
 * <p>Declaring behaviour as an interface (Open/Closed principle) ensures the
 * service layer can be extended or swapped without altering callers.</p>
 */
public interface FeatureService {

    /**
     * Returns feature flags for a specific role as a map of feature key to enabled status.
     *
     * @param role the user role (e.g. {@code ADMIN}, {@code RECEPTIONIST})
     * @return map where keys are feature identifiers and values indicate
     *         whether the feature is enabled for this role
     */
    Map<String, Boolean> getFeaturesByRole(String role);
}
