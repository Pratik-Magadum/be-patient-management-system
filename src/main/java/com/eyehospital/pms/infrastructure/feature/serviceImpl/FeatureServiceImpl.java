package com.eyehospital.pms.infrastructure.feature.serviceImpl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eyehospital.pms.infrastructure.feature.entity.Feature;
import com.eyehospital.pms.infrastructure.feature.repository.FeatureRepository;
import com.eyehospital.pms.infrastructure.feature.service.FeatureService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link FeatureService}.
 *
 * <p>Responsibilities (Single Responsibility principle):</p>
 * <ul>
 *   <li>Delegate persistence operations to {@link FeatureRepository}.</li>
 *   <li>Map the list of {@link Feature} entities to a key→enabled map.</li>
 * </ul>
 *
 * <p>Constructor injection is used exclusively — no field injection.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureServiceImpl implements FeatureService {

    private final FeatureRepository featureRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Boolean> getFeaturesByRole(String role) {
        log.debug("Fetching feature flags for role: {}", role);

        Map<String, Boolean> features = new LinkedHashMap<>();
        for (Feature feature : featureRepository.findByRole(role)) {
            features.put(feature.getFeatureKey(), feature.isEnabled());
        }

        log.debug("Returning {} feature flags for role '{}'", features.size(), role);
        return features;
    }
}
