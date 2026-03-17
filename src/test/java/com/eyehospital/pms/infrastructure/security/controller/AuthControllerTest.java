package com.eyehospital.pms.infrastructure.security.controller;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eyehospital.pms.BaseIntegrationTest;
import com.eyehospital.pms.infrastructure.security.entity.RefreshToken;
import com.eyehospital.pms.infrastructure.security.entity.User;
import com.eyehospital.pms.infrastructure.security.repository.RefreshTokenRepository;
import com.eyehospital.pms.infrastructure.security.repository.UserRepository;
import com.eyehospital.pms.infrastructure.security.service.JwtService;

/**
 * Integration tests for {@link AuthController}.
 *
 * <p>Filters are enabled so the full security filter chain (OAuth2 resource server,
 * {@code TenantContextFilter}) is exercised on every request — mirroring production behaviour.</p>
 *
 * <p>Test data is created in {@code @BeforeEach} and rolled back after each test
 * thanks to {@code @Transactional} on {@link BaseIntegrationTest}.</p>
 */
@DisplayName("AuthController — Integration Tests")
@AutoConfigureMockMvc(addFilters = true)
class AuthControllerTest extends BaseIntegrationTest {

    private static final String LOGIN_URL   = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL  = "/api/v1/auth/logout";

    private static final String TEST_USERNAME  = "testadmin";
    private static final String TEST_PASSWORD  = "SecureP@ss123";
    private static final UUID   HOSPITAL_ID    = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create a test user for each test (rolled back by @Transactional)
        testUser = new User();
        testUser.setHospitalId(HOSPITAL_ID);
        testUser.setUsername(TEST_USERNAME);
        testUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        testUser.setFullName("Test Admin");
        testUser.setEmail("testadmin_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com");
        testUser.setRole("ADMIN");
        testUser.setActive(true);
        testUser = userRepository.saveAndFlush(testUser);
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    private String loginRequestJson(String username, String password) {
        return """
                {"username": "%s", "password": "%s"}
                """.formatted(username, password);
    }

    private String refreshTokenRequestJson(String refreshToken) {
        return """
                {"refreshToken": "%s"}
                """.formatted(refreshToken);
    }

    /**
     * Performs a login and returns the raw refresh token from the response.
     */
    private String performLoginAndGetRefreshToken() throws Exception {
        String responseBody = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract refreshToken from JSON: {"data":{"refreshToken":"..."}}
        int start = responseBody.indexOf("\"refreshToken\":\"") + "\"refreshToken\":\"".length();
        int end = responseBody.indexOf("\"", start);
        return responseBody.substring(start, end);
    }

    // =======================================================================
    // POST /api/v1/auth/login
    // =======================================================================

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("returns 200 with tokens for valid credentials")
        void login_ValidCredentials_Returns200WithTokens() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson(TEST_USERNAME, TEST_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Login successful"))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").isNumber())
                    .andExpect(jsonPath("$.data.role").value("ADMIN"))
                    .andExpect(jsonPath("$.data.hospitalId").value(HOSPITAL_ID.toString()))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("returns 404 for non-existent username")
        void login_UnknownUsername_Returns404() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson("nonexistent_user", TEST_PASSWORD)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("not found")));
        }

        @Test
        @DisplayName("returns 409 for wrong password")
        void login_WrongPassword_Returns409() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson(TEST_USERNAME, "wrong_password")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("Invalid username or password")));
        }

        @Test
        @DisplayName("returns 400 when username is blank")
        void login_BlankUsername_Returns400() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson("", TEST_PASSWORD)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Username is required")));
        }

        @Test
        @DisplayName("returns 400 when password is blank")
        void login_BlankPassword_Returns400() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson(TEST_USERNAME, "")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Password is required")));
        }

        @Test
        @DisplayName("returns 400 when request body is empty JSON")
        void login_EmptyBody_Returns400() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 404 for inactive user")
        void login_InactiveUser_Returns404() throws Exception {
            testUser.setActive(false);
            userRepository.saveAndFlush(testUser);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson(TEST_USERNAME, TEST_PASSWORD)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("not found")));
        }

        @Test
        @DisplayName("second login succeeds and returns new tokens")
        void login_SecondLogin_ReturnsNewTokens() throws Exception {
            // First login
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson(TEST_USERNAME, TEST_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

            // Second login — should also succeed (old tokens are cleaned up)
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson(TEST_USERNAME, TEST_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("access token contains expected JWT claims")
        void login_ValidCredentials_AccessTokenContainsExpectedClaims() throws Exception {
            String responseBody = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson(TEST_USERNAME, TEST_PASSWORD)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Extract accessToken
            int start = responseBody.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
            int end = responseBody.indexOf("\"", start);
            String accessToken = responseBody.substring(start, end);

            // Verify claims via JwtService
            var jwt = jwtService.decode(accessToken);
            assert jwt.getSubject().equals(TEST_USERNAME);
            assert jwt.getClaimAsString("role").equals("ADMIN");
            assert jwt.getClaimAsString("hospitalId").equals(HOSPITAL_ID.toString());
            assert jwt.getClaimAsString("userId").equals(testUser.getUserId().toString());
            assert jwt.getClaimAsString("tokenType").equals("ACCESS");
        }

        @Test
        @DisplayName("login works for each role type")
        void login_AllRoles_ReturnsCorrectRole() throws Exception {
            for (String role : new String[]{"RECEPTIONIST", "ASSISTANT", "DOCTOR"}) {
                User roleUser = new User();
                roleUser.setHospitalId(HOSPITAL_ID);
                roleUser.setUsername("user_" + role.toLowerCase());
                roleUser.setPassword(passwordEncoder.encode("pass123"));
                roleUser.setFullName(role + " User");
                roleUser.setEmail(role.toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com");
                roleUser.setRole(role);
                roleUser.setActive(true);
                userRepository.saveAndFlush(roleUser);

                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequestJson("user_" + role.toLowerCase(), "pass123")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.role").value(role));
            }
        }
    }

    // =======================================================================
    // POST /api/v1/auth/refresh
    // =======================================================================

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("returns 200 with new access token for valid refresh token")
        void refresh_ValidToken_Returns200WithNewAccessToken() throws Exception {
            String refreshToken = performLoginAndGetRefreshToken();

            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(refreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").value(refreshToken))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").isNumber())
                    .andExpect(jsonPath("$.data.role").value("ADMIN"))
                    .andExpect(jsonPath("$.data.hospitalId").value(HOSPITAL_ID.toString()));
        }

        @Test
        @DisplayName("returns 409 for revoked refresh token")
        void refresh_RevokedToken_Returns409() throws Exception {
            String refreshToken = performLoginAndGetRefreshToken();

            // Revoke the token
            RefreshToken storedToken = refreshTokenRepository
                    .findByTokenAndRevokedFalse(refreshToken).orElseThrow();
            storedToken.setRevoked(true);
            refreshTokenRepository.saveAndFlush(storedToken);

            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(refreshToken)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("Invalid or revoked refresh token")));
        }

        @Test
        @DisplayName("returns 409 for expired refresh token")
        void refresh_ExpiredToken_Returns409() throws Exception {
            String refreshToken = performLoginAndGetRefreshToken();

            // Force-expire the token
            RefreshToken storedToken = refreshTokenRepository
                    .findByTokenAndRevokedFalse(refreshToken).orElseThrow();
            storedToken.setExpiresAt(Instant.now().minusSeconds(60));
            refreshTokenRepository.saveAndFlush(storedToken);

            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(refreshToken)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("Refresh token expired")));
        }

        @Test
        @DisplayName("returns 409 for non-existent refresh token")
        void refresh_NonExistentToken_Returns409() throws Exception {
            // Generate a valid JWT that is NOT stored in the database
            String fakeRefreshToken = jwtService.generateRefreshToken(testUser.getUserId(), TEST_USERNAME);

            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(fakeRefreshToken)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("Invalid or revoked refresh token")));
        }

        @Test
        @DisplayName("returns 400 when refreshToken field is blank")
        void refresh_BlankToken_Returns400() throws Exception {
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson("")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Refresh token is required")));
        }

        @Test
        @DisplayName("returns 400 when request body is empty JSON")
        void refresh_EmptyBody_Returns400() throws Exception {
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("new access token after refresh has correct claims")
        void refresh_ValidToken_NewAccessTokenHasCorrectClaims() throws Exception {
            String refreshToken = performLoginAndGetRefreshToken();

            String responseBody = mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(refreshToken)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Extract new access token and verify claims
            int start = responseBody.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
            int end = responseBody.indexOf("\"", start);
            String newAccessToken = responseBody.substring(start, end);

            var jwt = jwtService.decode(newAccessToken);
            assert jwt.getSubject().equals(TEST_USERNAME);
            assert jwt.getClaimAsString("role").equals("ADMIN");
            assert jwt.getClaimAsString("hospitalId").equals(HOSPITAL_ID.toString());
            assert jwt.getClaimAsString("tokenType").equals("ACCESS");
        }

        @Test
        @DisplayName("returns 404 when user is deactivated between login and refresh")
        void refresh_UserDeactivatedAfterLogin_Returns404() throws Exception {
            String refreshToken = performLoginAndGetRefreshToken();

            // Deactivate user after login
            testUser.setActive(false);
            userRepository.saveAndFlush(testUser);

            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(refreshToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("not found")));
        }
    }

    // =======================================================================
    // POST /api/v1/auth/logout
    // =======================================================================

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("returns 200 and revokes the refresh token")
        void logout_ValidToken_Returns200AndRevokesToken() throws Exception {
            String refreshToken = performLoginAndGetRefreshToken();

            mockMvc.perform(post(LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(refreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));

            // Verify token is now revoked — refresh should fail
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(refreshToken)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 200 even for a non-existent token (idempotent)")
        void logout_NonExistentToken_Returns200() throws Exception {
            String fakeToken = jwtService.generateRefreshToken(testUser.getUserId(), TEST_USERNAME);

            mockMvc.perform(post(LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(fakeToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }

        @Test
        @DisplayName("returns 200 for already-revoked token (idempotent)")
        void logout_AlreadyRevokedToken_Returns200() throws Exception {
            String refreshToken = performLoginAndGetRefreshToken();

            // Logout once
            mockMvc.perform(post(LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(refreshToken)))
                    .andExpect(status().isOk());

            // Logout again — should still succeed
            mockMvc.perform(post(LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(refreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }

        @Test
        @DisplayName("returns 400 when refreshToken field is blank")
        void logout_BlankToken_Returns400() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson("")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Refresh token is required")));
        }

        @Test
        @DisplayName("returns 400 when request body is empty JSON")
        void logout_EmptyBody_Returns400() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =======================================================================
    // Security filter chain — auth endpoints are public
    // =======================================================================

    @Nested
    @DisplayName("Security filter chain — public access")
    class SecurityFilterChainPublicAccess {

        @Test
        @DisplayName("login endpoint is accessible without Bearer token")
        void login_NoBearerToken_StillAccessible() throws Exception {
            // No Authorization header → should still reach the controller
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson(TEST_USERNAME, TEST_PASSWORD)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("refresh endpoint is accessible without Bearer token")
        void refresh_NoBearerToken_StillAccessible() throws Exception {
            String refreshToken = performLoginAndGetRefreshToken();

            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(refreshToken)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("logout endpoint is accessible without Bearer token")
        void logout_NoBearerToken_StillAccessible() throws Exception {
            String refreshToken = performLoginAndGetRefreshToken();

            mockMvc.perform(post(LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(refreshToken)))
                    .andExpect(status().isOk());
        }
    }

    // =======================================================================
    // Security filter chain — protected endpoints require valid JWT
    // =======================================================================

    @Nested
    @DisplayName("Security filter chain — protected endpoints")
    class SecurityFilterChainProtectedEndpoints {

        @Test
        @DisplayName("protected endpoint returns 401 without Bearer token")
        void protectedEndpoint_NoBearerToken_Returns401() throws Exception {
            mockMvc.perform(post("/api/v1/patients")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Unauthorized"));
        }

        @Test
        @DisplayName("protected endpoint returns 401 with malformed Bearer token")
        void protectedEndpoint_MalformedToken_Returns401() throws Exception {
            mockMvc.perform(post("/api/v1/patients")
                            .header("Authorization", "Bearer invalid.token.here")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("auth endpoints only accept POST — GET is not mapped")
        void authEndpoints_GetMethod_ReturnsError() throws Exception {
            mockMvc.perform(get(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("protected endpoint accessible with valid Bearer token (not 401)")
        void protectedEndpoint_ValidToken_DoesNotReturn401() throws Exception {
            // Login to get a valid access token
            String responseBody = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson(TEST_USERNAME, TEST_PASSWORD)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            int start = responseBody.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
            int end = responseBody.indexOf("\"", start);
            String accessToken = responseBody.substring(start, end);

            // Use the valid token — should pass security filters (TenantContextFilter sets hospitalId)
            // May return 404 since no patient controller exists, but NOT 401
            int statusCode = mockMvc.perform(get("/api/v1/patients")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            // Any status except 401 means the JWT was accepted by the security filter chain
            assertTrue(statusCode != 401,
                    "Expected non-401 status with valid Bearer token, got " + statusCode);
        }
    }

    // =======================================================================
    // TenantContextFilter — hospitalId extraction from JWT
    // =======================================================================

    @Nested
    @DisplayName("TenantContextFilter — hospitalId in JWT")
    class TenantContextFilterTests {

        @Test
        @DisplayName("login response contains correct hospitalId from user record")
        void login_ValidCredentials_ResponseContainsCorrectHospitalId() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson(TEST_USERNAME, TEST_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hospitalId").value(HOSPITAL_ID.toString()));
        }

        @Test
        @DisplayName("access token JWT contains hospitalId claim")
        void login_ValidCredentials_JwtContainsHospitalIdClaim() throws Exception {
            String responseBody = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson(TEST_USERNAME, TEST_PASSWORD)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            int start = responseBody.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
            int end = responseBody.indexOf("\"", start);
            String accessToken = responseBody.substring(start, end);

            String hospitalIdFromToken = jwtService.extractHospitalId(accessToken);
            assert HOSPITAL_ID.toString().equals(hospitalIdFromToken)
                    : "Expected hospitalId " + HOSPITAL_ID + " but got " + hospitalIdFromToken;
        }

        @Test
        @DisplayName("users from different hospitals get their own hospitalId in token")
        void login_DifferentHospital_TokenContainsCorrectHospitalId() throws Exception {
            UUID otherHospitalId = UUID.fromString("b1ffcd99-9c0b-4ef8-bb6d-6bb9bd380a22");
            User otherUser = new User();
            otherUser.setHospitalId(otherHospitalId);
            otherUser.setUsername("otherhospitaladmin");
            otherUser.setPassword(passwordEncoder.encode("pass123"));
            otherUser.setFullName("Other Admin");
            otherUser.setEmail("other_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com");
            otherUser.setRole("ADMIN");
            otherUser.setActive(true);
            userRepository.saveAndFlush(otherUser);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson("otherhospitaladmin", "pass123")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hospitalId").value(otherHospitalId.toString()));
        }
    }

    // =======================================================================
    // Content-Type and request format validation
    // =======================================================================

    @Nested
    @DisplayName("Request format validation")
    class RequestFormatValidation {

        @Test
        @DisplayName("returns error when Content-Type is not application/json")
        void login_WrongContentType_ReturnsError() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(loginRequestJson(TEST_USERNAME, TEST_PASSWORD)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("returns error when request body is malformed JSON")
        void login_MalformedJson_ReturnsError() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("returns error for missing request body")
        void login_NoBody_ReturnsError() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());
        }
    }

    // =======================================================================
    // Full login → refresh → logout flow
    // =======================================================================

    @Nested
    @DisplayName("End-to-end auth flow")
    class EndToEndAuthFlow {

        @Test
        @DisplayName("login → refresh → logout → refresh fails")
        void fullFlow_LoginRefreshLogoutRefreshFails() throws Exception {
            // 1. Login
            String refreshToken = performLoginAndGetRefreshToken();

            // 2. Refresh — should succeed
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(refreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

            // 3. Logout
            mockMvc.perform(post(LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(refreshToken)))
                    .andExpect(status().isOk());

            // 4. Refresh again — should fail (token revoked)
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshTokenRequestJson(refreshToken)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("Invalid or revoked refresh token")));
        }

        @Test
        @DisplayName("login → login again → second login succeeds with fresh tokens")
        void fullFlow_ReLoginSucceeds() throws Exception {
            // 1. First login
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson(TEST_USERNAME, TEST_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

            // 2. Second login — succeeds and returns new tokens
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson(TEST_USERNAME, TEST_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.role").value("ADMIN"));
        }
    }

    // =======================================================================
    // JwtService — isTokenValid coverage
    // =======================================================================

    @Nested
    @DisplayName("JwtService — isTokenValid")
    class JwtServiceIsTokenValid {

        @Test
        @DisplayName("returns true for a valid access token")
        void isTokenValid_ValidAccessToken_ReturnsTrue() throws Exception {
            String accessToken = jwtService.generateAccessToken(
                    testUser.getUserId(), TEST_USERNAME, HOSPITAL_ID, "ADMIN");
            assertTrue(jwtService.isTokenValid(accessToken));
        }

        @Test
        @DisplayName("returns true for a valid refresh token")
        void isTokenValid_ValidRefreshToken_ReturnsTrue() throws Exception {
            String refreshToken = jwtService.generateRefreshToken(testUser.getUserId(), TEST_USERNAME);
            assertTrue(jwtService.isTokenValid(refreshToken));
        }

        @Test
        @DisplayName("returns false for a tampered token")
        void isTokenValid_TamperedToken_ReturnsFalse() {
            String validToken = jwtService.generateAccessToken(
                    testUser.getUserId(), TEST_USERNAME, HOSPITAL_ID, "ADMIN");
            // Tamper with the token by altering characters in the signature
            String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";
            assertFalse(jwtService.isTokenValid(tamperedToken));
        }

        @Test
        @DisplayName("returns false for a completely invalid token string")
        void isTokenValid_GarbageString_ReturnsFalse() {
            assertFalse(jwtService.isTokenValid("not.a.valid.jwt.token"));
        }

        @Test
        @DisplayName("returns false for an empty string")
        void isTokenValid_EmptyString_ReturnsFalse() {
            assertFalse(jwtService.isTokenValid(""));
        }
    }
}
