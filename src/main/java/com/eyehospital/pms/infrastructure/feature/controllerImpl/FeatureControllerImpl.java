package com.eyehospital.pms.infrastructure.feature.controllerImpl;

import java.util.Map;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

import com.eyehospital.pms.infrastructure.feature.controller.FeatureController;
import com.eyehospital.pms.infrastructure.feature.service.FeatureService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller implementation for feature-flag endpoints.
 *
 * <p>This class is intentionally thin — it only delegates to
 * {@link FeatureService} for business logic and returns the result directly.</p>
 *
 * <p>All Swagger annotations are declared on the {@link FeatureController}
 * interface. Constructor injection is used — no field injection.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class FeatureControllerImpl implements FeatureController {

    private final FeatureService featureService;

    @Override
    public Map<String, Boolean> getFeatures(JwtAuthenticationToken authentication) {
        String role = authentication.getToken().getClaimAsString("role");
        log.info("GET /features - fetching feature flags for role '{}'", role);
        return featureService.getFeaturesByRole(role);
    }
}
