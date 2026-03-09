package com.eyehospital.pms.infrastructure.tenant;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.eyehospital.pms.BaseIntegrationTest;
import com.eyehospital.pms.common.constants.ApiConstants;

/**
 * Integration tests for the Hospital API endpoint.
 *
 * <p>The full Spring application context is loaded against a real PostgreSQL
 * instance managed by Testcontainers. The test database is seeded via
 * Liquibase using the test changelogs (which mirror the production DDL and
 * include demo hospital rows via DML).</p>
 *
 * <p>Endpoint under test: {@code GET /api/v1/hospitals/{subdomain}}</p>
 *
 * <p>Naming convention: {@code methodName_StateUnderTest_ExpectedBehavior}</p>
 */
@DisplayName("HospitalController — Integration Tests")
class HospitalControllerIntegrationTest extends BaseIntegrationTest {

    private static final String HOSPITAL_URL = ApiConstants.HOSPITALS + "/{subdomain}";

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/hospitals/{subdomain}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/hospitals/{subdomain}")
    class GetHospitalBySubdomain {

        @Test
        @DisplayName("returns 200 OK with hospital data for a known active subdomain")
        void getHospitalBySubdomain_KnownActiveSubdomain_Returns200WithData() throws Exception {
            // ACT & ASSERT — "apollo-eye" is inserted by the test DML seed script
            mockMvc.perform(get(HOSPITAL_URL, "apollo-eye").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.subdomain").value("apollo-eye"))
                    .andExpect(jsonPath("$.message").value("Hospital retrieved successfully"));
        }

        @Test
        @DisplayName("response body contains expected API envelope fields")
        void getHospitalBySubdomain_KnownSubdomain_ResponseContainsEnvelopeFields() throws Exception {
            // ACT & ASSERT — verify ApiResponse<T> envelope: status, message, data, timestamp
            mockMvc.perform(get(HOSPITAL_URL, "apollo-eye").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("response data contains expected hospital fields")
        void getHospitalBySubdomain_KnownSubdomain_DataContainsHospitalFields() throws Exception {
            // ACT & ASSERT — verify HospitalResponseDto fields present in JSON
            mockMvc.perform(get(HOSPITAL_URL, "apollo-eye").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hospitalId").exists())
                    .andExpect(jsonPath("$.data.name").exists())
                    .andExpect(jsonPath("$.data.subdomain").exists())
                    .andExpect(jsonPath("$.data.contactEmail").exists())
                    .andExpect(jsonPath("$.data.contactPhone").exists())
                    .andExpect(jsonPath("$.data.active").exists());
        }

        @Test
        @DisplayName("returns 404 NOT FOUND for an unknown subdomain")
        void getHospitalBySubdomain_UnknownSubdomain_Returns404() throws Exception {
            mockMvc.perform(get(HOSPITAL_URL, "completely-unknown-xyz").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(containsString("not found")));
        }

        @Test
        @DisplayName("returns 404 NOT FOUND for an inactive hospital subdomain")
        void getHospitalBySubdomain_InactiveSubdomain_Returns404() throws Exception {
            // "inactive-hospital" exists in DB but is_active = false (seeded by test DML)
            mockMvc.perform(get(HOSPITAL_URL, "inactive-hospital").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }
}
