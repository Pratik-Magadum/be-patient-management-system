package com.eyehospital.pms.infrastructure.feature;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eyehospital.pms.BaseIntegrationTest;
import com.eyehospital.pms.common.constants.ApiConstants;
import com.eyehospital.pms.infrastructure.security.entity.User;
import com.eyehospital.pms.infrastructure.security.repository.UserRepository;
import com.eyehospital.pms.infrastructure.security.service.JwtService;
import com.eyehospital.pms.infrastructure.tenant.entity.Hospital;
import com.eyehospital.pms.infrastructure.tenant.repository.HospitalRepository;

/**
 * Integration tests for the Feature Flags API endpoint.
 *
 * <p>The full Spring application context is loaded against a real PostgreSQL
 * instance managed by Testcontainers. The test database is seeded via
 * Liquibase with role-based feature flags.</p>
 *
 * <p>Filters are enabled so the full security filter chain (OAuth2 resource server,
 * {@code TenantContextFilter}) is exercised — mirroring production behaviour.
 * The role is extracted from the JWT token.</p>
 *
 * <p>Endpoint under test: {@code GET /api/v1/features}</p>
 *
 * <p>Naming convention: {@code methodName_StateUnderTest_ExpectedBehavior}</p>
 */
@DisplayName("FeatureController — Integration Tests")
@AutoConfigureMockMvc(addFilters = true)
class FeatureControllerIntegrationTest extends BaseIntegrationTest {

    private static final String FEATURES_URL = ApiConstants.FEATURES;

    @Autowired private UserRepository userRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private UUID hospitalId;

    @BeforeEach
    void setUp() {
        Hospital hospital = new Hospital();
        hospital.setName("Feature Test Hospital");
        hospital.setSubdomain("feature-test-" + UUID.randomUUID().toString().substring(0, 8));
        hospital.setAddress("123 Test Street");
        hospital.setContactEmail("test@feature.com");
        hospital.setContactPhone("+91-1234567890");
        hospital.setActive(true);
        hospital = hospitalRepository.saveAndFlush(hospital);
        hospitalId = hospital.getHospitalId();
    }

    private String createTokenForRole(String role) {
        User user = new User();
        user.setHospitalId(hospitalId);
        user.setUsername("feature-" + role.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8));
        user.setPassword(passwordEncoder.encode("TestP@ss123"));
        user.setFullName("Test " + role);
        user.setEmail("feature_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com");
        user.setRole(role);
        user.setActive(true);
        user = userRepository.saveAndFlush(user);

        return jwtService.generateAccessToken(
                user.getUserId(), user.getUsername(), hospitalId, user.getRole());
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/features — ADMIN
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/features (ADMIN token)")
    class GetFeaturesForAdmin {

        @Test
        @DisplayName("returns 200 OK with all features enabled for ADMIN")
        void getFeatures_AdminRole_Returns200WithAllEnabled() throws Exception {
            String token = createTokenForRole("ADMIN");

            mockMvc.perform(get(FEATURES_URL)
                            .header("Authorization", "Bearer " + token)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.SEARCH_PATIENTS").value(true))
                    .andExpect(jsonPath("$.DATE_RANGE_FILTER").value(true))
                    .andExpect(jsonPath("$.STATS_CARDS").value(true))
                    .andExpect(jsonPath("$.ADD_NEW_PATIENT").value(true))
                    .andExpect(jsonPath("$.EDIT_PATIENT").value(true))
                    .andExpect(jsonPath("$.DELETE_PATIENT").value(true))
                    .andExpect(jsonPath("$.FOLLOW_UP").value(true))
                    .andExpect(jsonPath("$.STATUS_MANAGEMENT").value(true))
                    .andExpect(jsonPath("$.PAGINATION").value(true));
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/features — RECEPTIONIST
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/features (RECEPTIONIST token)")
    class GetFeaturesForReceptionist {

        @Test
        @DisplayName("returns enabled features for RECEPTIONIST role")
        void getFeatures_ReceptionistRole_ReturnsCorrectFlags() throws Exception {
            String token = createTokenForRole("RECEPTIONIST");

            mockMvc.perform(get(FEATURES_URL)
                            .header("Authorization", "Bearer " + token)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.SEARCH_PATIENTS").value(true))
                    .andExpect(jsonPath("$.ADD_NEW_PATIENT").value(true))
                    .andExpect(jsonPath("$.FOLLOW_UP").value(true))
                    .andExpect(jsonPath("$.EDIT_PATIENT").value(false))
                    .andExpect(jsonPath("$.DELETE_PATIENT").value(false));
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/features — DOCTOR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/features (DOCTOR token)")
    class GetFeaturesForDoctor {

        @Test
        @DisplayName("returns enabled features for DOCTOR role")
        void getFeatures_DoctorRole_ReturnsCorrectFlags() throws Exception {
            String token = createTokenForRole("DOCTOR");

            mockMvc.perform(get(FEATURES_URL)
                            .header("Authorization", "Bearer " + token)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.SEARCH_PATIENTS").value(true))
                    .andExpect(jsonPath("$.FOLLOW_UP").value(true))
                    .andExpect(jsonPath("$.ADD_NEW_PATIENT").value(false))
                    .andExpect(jsonPath("$.EDIT_PATIENT").value(false))
                    .andExpect(jsonPath("$.DELETE_PATIENT").value(false));
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/features — ASSISTANT
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/features (ASSISTANT token)")
    class GetFeaturesForAssistant {

        @Test
        @DisplayName("returns enabled features for ASSISTANT role")
        void getFeatures_AssistantRole_ReturnsCorrectFlags() throws Exception {
            String token = createTokenForRole("ASSISTANT");

            mockMvc.perform(get(FEATURES_URL)
                            .header("Authorization", "Bearer " + token)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.SEARCH_PATIENTS").value(true))
                    .andExpect(jsonPath("$.STATUS_MANAGEMENT").value(true))
                    .andExpect(jsonPath("$.PAGINATION").value(true))
                    .andExpect(jsonPath("$.DATE_RANGE_FILTER").value(false))
                    .andExpect(jsonPath("$.ADD_NEW_PATIENT").value(false))
                    .andExpect(jsonPath("$.FOLLOW_UP").value(false));
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/features — Unauthorized
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/features — security")
    class GetFeaturesSecurity {

        @Test
        @DisplayName("returns 401 when no token is provided")
        void getFeatures_NoToken_Returns401() throws Exception {
            mockMvc.perform(get(FEATURES_URL).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }
}
