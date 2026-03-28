package com.eyehospital.pms.module.patient.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.eyehospital.pms.module.appointment.entity.Appointment;
import com.eyehospital.pms.module.appointment.repository.AppointmentRepository;
import com.eyehospital.pms.module.patient.entity.Patient;
import com.eyehospital.pms.module.patient.repository.PatientRepository;

/**
 * Integration tests for the Patient module endpoints:
 * <ul>
 *   <li>{@code GET /api/v1/patients/dashboard/today} — dashboard</li>
 *   <li>{@code GET /api/v1/patients/search} — search by dates, status, visit type</li>
 *   <li>{@code GET /api/v1/patients/search/by-name-phone} — search by name/phone</li>
 *   <li>{@code DELETE /api/v1/patients/{id}} — soft delete patient</li>
 *   <li>{@code DELETE /api/v1/appointments/{id}} — soft delete appointment</li>
 * </ul>
 *
 * <p>Filters are enabled so the full security filter chain (OAuth2 resource server,
 * {@code TenantContextFilter}) is exercised — mirroring production behaviour.</p>
 *
 * <p>Test data is created in {@code @BeforeEach} and rolled back after each test
 * thanks to {@code @Transactional} on {@link BaseIntegrationTest}.</p>
 *
 * <p>Naming convention: {@code methodName_StateUnderTest_ExpectedBehavior}</p>
 */
@DisplayName("PatientController Integration Tests")
@AutoConfigureMockMvc(addFilters = true)
class PatientControllerIntegrationTest extends BaseIntegrationTest {

    private static final String DASHBOARD_URL =
            ApiConstants.PATIENTS + ApiConstants.PATIENT_DASHBOARD_TODAY;
    private static final String SEARCH_URL =
            ApiConstants.PATIENTS + ApiConstants.PATIENT_BY_DATES;

    private static final String TEST_USERNAME = "dashboard-admin";
    private static final String TEST_PASSWORD = "SecureP@ss123";

    @Autowired private UserRepository userRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private UUID hospitalId;
    private String accessToken;

    @BeforeEach
    void setUp() {
        // 1. Create a test hospital
        Hospital hospital = new Hospital();
        hospital.setName("Test Eye Hospital");
        hospital.setSubdomain("test-dashboard-" + UUID.randomUUID().toString().substring(0, 8));
        hospital.setAddress("123 Test Street");
        hospital.setContactEmail("test@dashboard.com");
        hospital.setContactPhone("+91-1234567890");
        hospital.setActive(true);
        hospital = hospitalRepository.saveAndFlush(hospital);
        hospitalId = hospital.getHospitalId();

        // 2. Create a test user linked to the hospital
        User user = new User();
        user.setHospitalId(hospitalId);
        user.setUsername(TEST_USERNAME + "-" + UUID.randomUUID().toString().substring(0, 8));
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setFullName("Dashboard Admin");
        user.setEmail("dashboard_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com");
        user.setRole("ADMIN");
        user.setActive(true);
        user = userRepository.saveAndFlush(user);

        // 3. Generate an access token for the test user
        accessToken = jwtService.generateAccessToken(
                user.getUserId(), user.getUsername(), hospitalId, user.getRole());
    }

    // =======================================================================
    // Helper methods
    // =======================================================================

    private Patient createPatient(String fullName, String mobile) {
        Patient patient = new Patient();
        patient.setHospital(hospitalRepository.getReferenceById(hospitalId));
        patient.setFullName(fullName);
        patient.setMobileNumber(mobile);
        patient.setAge(35);
        patient.setGender("MALE");
        patient.setEmail(fullName.toLowerCase().replace(" ", ".") + "@test.com");
        patient.setDateOfBirth(LocalDate.of(1990, 5, 15));
        patient.setAddress("123 Test Street");
        return patientRepository.saveAndFlush(patient);
    }

    private Appointment createAppointment(Patient patient, String visitType,
                                          String status, LocalDate date, LocalTime time) {
        Appointment appt = new Appointment();
        appt.setHospitalId(hospitalId);
        appt.setPatient(patient);
        appt.setAppointmentDate(date);
        appt.setAppointmentTime(time);
        appt.setVisitType(visitType);
        appt.setStatus(status);

        if ("FOLLOW_UP".equals(visitType)) {
            Appointment parent = new Appointment();
            parent.setHospitalId(hospitalId);
            parent.setPatient(patient);
            parent.setAppointmentDate(date.minusDays(7));
            parent.setAppointmentTime(time);
            parent.setVisitType("NEW_VISIT");
            parent.setStatus("COMPLETED");
            parent = appointmentRepository.saveAndFlush(parent);
            appt.setParentAppointment(parent);
        }

        return appointmentRepository.saveAndFlush(appt);
    }

    // =======================================================================
    // GET /api/v1/patients/dashboard/today — success scenarios
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/dashboard/today — success")
    class GetTodayDashboardSuccess {

        @Test
        @DisplayName("returns 200 with today's date when no appointments exist")
        void getTodayDashboard_NoAppointments_ReturnsZeroCounts() throws Exception {
            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.date").value(LocalDate.now().toString()))
                    .andExpect(jsonPath("$.totalPatients").value(0))
                    .andExpect(jsonPath("$.newPatients").value(0))
                    .andExpect(jsonPath("$.followUpPatients").value(0))
                    .andExpect(jsonPath("$.completedPatients").value(0))
                    .andExpect(jsonPath("$.totalRegisteredPatients").value(0));
        }

        @Test
        @DisplayName("returns correct counts for new visit appointments")
        void getTodayDashboard_NewVisits_ReturnsCorrectNewCount() throws Exception {
            Patient p1 = createPatient("Patient One", "+91-1000000001");
            Patient p2 = createPatient("Patient Two", "+91-1000000002");

            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "COMPLETED", LocalDate.now(), LocalTime.of(10, 0));

            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPatients").value(2))
                    .andExpect(jsonPath("$.newPatients").value(2))
                    .andExpect(jsonPath("$.followUpPatients").value(0))
                    .andExpect(jsonPath("$.completedPatients").value(1));
        }

        @Test
        @DisplayName("returns correct counts for follow-up appointments")
        void getTodayDashboard_FollowUps_ReturnsCorrectFollowUpCount() throws Exception {
            Patient p1 = createPatient("Follow-up One", "+91-2000000001");
            Patient p2 = createPatient("Follow-up Two", "+91-2000000002");

            createAppointment(p1, "FOLLOW_UP", "COMPLETED", LocalDate.now(), LocalTime.of(14, 0));
            createAppointment(p2, "FOLLOW_UP", "IN_PROGRESS", LocalDate.now(), LocalTime.of(14, 30));

            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPatients").value(2))
                    .andExpect(jsonPath("$.newPatients").value(0))
                    .andExpect(jsonPath("$.followUpPatients").value(2))
                    .andExpect(jsonPath("$.completedPatients").value(1));
        }

        @Test
        @DisplayName("returns correct mixed counts for new + follow-up + completed")
        void getTodayDashboard_MixedAppointments_ReturnsCorrectAggregation() throws Exception {
            Patient p1 = createPatient("New Completed", "+91-3000000001");
            Patient p2 = createPatient("New InProgress", "+91-3000000002");
            Patient p3 = createPatient("FollowUp Completed", "+91-3000000003");
            Patient p4 = createPatient("FollowUp Registered", "+91-3000000004");

            createAppointment(p1, "NEW_VISIT", "COMPLETED", LocalDate.now(), LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "IN_PROGRESS", LocalDate.now(), LocalTime.of(10, 0));
            createAppointment(p3, "FOLLOW_UP", "COMPLETED", LocalDate.now(), LocalTime.of(14, 0));
            createAppointment(p4, "FOLLOW_UP", "REGISTERED", LocalDate.now(), LocalTime.of(15, 0));

            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.date").value(LocalDate.now().toString()))
                    .andExpect(jsonPath("$.totalPatients").value(4))
                    .andExpect(jsonPath("$.newPatients").value(2))
                    .andExpect(jsonPath("$.followUpPatients").value(2))
                    .andExpect(jsonPath("$.completedPatients").value(2));
        }

        @Test
        @DisplayName("totalRegisteredPatients counts all patients for the hospital")
        void getTodayDashboard_RegisteredPatients_CountsAllPatients() throws Exception {
            // Create patients, only some with today's appointments
            Patient p1 = createPatient("With Appointment", "+91-4000000001");
            createPatient("Without Appointment", "+91-4000000002");
            createPatient("Also No Appointment", "+91-4000000003");

            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPatients").value(1))
                    .andExpect(jsonPath("$.totalRegisteredPatients").value(3));
        }

        @Test
        @DisplayName("response contains all expected dashboard fields")
        void getTodayDashboard_ValidRequest_ResponseContainsAllFields() throws Exception {
            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.date").exists())
                    .andExpect(jsonPath("$.totalPatients").exists())
                    .andExpect(jsonPath("$.newPatients").exists())
                    .andExpect(jsonPath("$.followUpPatients").exists())
                    .andExpect(jsonPath("$.completedPatients").exists())
                    .andExpect(jsonPath("$.totalRegisteredPatients").exists());
        }
    }

    // =======================================================================
    // GET /api/v1/patients/dashboard/today — tenant isolation
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/dashboard/today — tenant isolation")
    class GetTodayDashboardTenantIsolation {

        @Test
        @DisplayName("dashboard only returns data for the authenticated user's hospital")
        void getTodayDashboard_MultiTenant_ReturnsOnlyOwnHospitalData() throws Exception {
            // Create a patient and appointment for the test hospital
            Patient ownPatient = createPatient("Own Hospital Patient", "+91-5000000001");
            createAppointment(ownPatient, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            // Create a different hospital with its own patient and appointment
            Hospital otherHospital = new Hospital();
            otherHospital.setName("Other Eye Hospital");
            otherHospital.setSubdomain("other-hosp-" + UUID.randomUUID().toString().substring(0, 8));
            otherHospital.setAddress("456 Other Street");
            otherHospital.setContactEmail("other@hospital.com");
            otherHospital.setContactPhone("+91-9999999999");
            otherHospital.setActive(true);
            otherHospital = hospitalRepository.saveAndFlush(otherHospital);

            Patient otherPatient = new Patient();
            otherPatient.setHospital(otherHospital);
            otherPatient.setFullName("Other Hospital Patient");
            otherPatient.setMobileNumber("+91-5000000002");
            otherPatient.setAge(40);
            otherPatient.setGender("FEMALE");
            otherPatient = patientRepository.saveAndFlush(otherPatient);

            Appointment otherAppt = new Appointment();
            otherAppt.setHospitalId(otherHospital.getHospitalId());
            otherAppt.setPatient(otherPatient);
            otherAppt.setAppointmentDate(LocalDate.now());
            otherAppt.setAppointmentTime(LocalTime.of(11, 0));
            otherAppt.setVisitType("NEW_VISIT");
            otherAppt.setStatus("REGISTERED");
            appointmentRepository.saveAndFlush(otherAppt);

            // Request with our hospital's token — should only see our data
            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPatients").value(1))
                    .andExpect(jsonPath("$.newPatients").value(1))
                    .andExpect(jsonPath("$.totalRegisteredPatients").value(1));
        }
    }

    // =======================================================================
    // GET /api/v1/patients/dashboard/today — authentication & authorization
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/dashboard/today — security")
    class GetTodayDashboardSecurity {

        @Test
        @DisplayName("returns 401 when no Bearer token is provided")
        void getTodayDashboard_NoToken_Returns401() throws Exception {
            mockMvc.perform(get(DASHBOARD_URL)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 401 when Bearer token is malformed")
        void getTodayDashboard_MalformedToken_Returns401() throws Exception {
            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer invalid.jwt.token")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("ADMIN role can access the dashboard")
        void getTodayDashboard_AdminRole_Returns200() throws Exception {
            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DOCTOR role can access the dashboard")
        void getTodayDashboard_DoctorRole_Returns200() throws Exception {
            String doctorToken = createTokenForRole("DOCTOR");
            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer " + doctorToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("RECEPTIONIST role can access the dashboard")
        void getTodayDashboard_ReceptionistRole_Returns200() throws Exception {
            String receptionistToken = createTokenForRole("RECEPTIONIST");
            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer " + receptionistToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ASSISTANT role can access the dashboard")
        void getTodayDashboard_AssistantRole_Returns200() throws Exception {
            String assistantToken = createTokenForRole("ASSISTANT");
            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer " + assistantToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        private String createTokenForRole(String role) {
            User user = new User();
            user.setHospitalId(hospitalId);
            user.setUsername(role.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8));
            user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
            user.setFullName("Test " + role);
            user.setEmail(role.toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com");
            user.setRole(role);
            user.setActive(true);
            user = userRepository.saveAndFlush(user);
            return jwtService.generateAccessToken(user.getUserId(), user.getUsername(), hospitalId, role);
        }
    }

    // =======================================================================
    // GET /api/v1/patients/dashboard/today — edge cases
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/dashboard/today — edge cases")
    class GetTodayDashboardEdgeCases {

        @Test
        @DisplayName("past appointments are not counted in today's dashboard")
        void getTodayDashboard_PastAppointments_NotCounted() throws Exception {
            Patient patient = createPatient("Past Patient", "+91-6000000001");

            // Create a past appointment (yesterday)
            Appointment pastAppt = new Appointment();
            pastAppt.setHospitalId(hospitalId);
            pastAppt.setPatient(patient);
            pastAppt.setAppointmentDate(LocalDate.now().minusDays(1));
            pastAppt.setAppointmentTime(LocalTime.of(9, 0));
            pastAppt.setVisitType("NEW_VISIT");
            pastAppt.setStatus("COMPLETED");
            appointmentRepository.saveAndFlush(pastAppt);

            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPatients").value(0))
                    .andExpect(jsonPath("$.completedPatients").value(0))
                    .andExpect(jsonPath("$.totalRegisteredPatients").value(1));
        }

        @Test
        @DisplayName("future appointments are not counted in today's dashboard")
        void getTodayDashboard_FutureAppointments_NotCounted() throws Exception {
            Patient patient = createPatient("Future Patient", "+91-6000000002");

            Appointment futureAppt = new Appointment();
            futureAppt.setHospitalId(hospitalId);
            futureAppt.setPatient(patient);
            futureAppt.setAppointmentDate(LocalDate.now().plusDays(1));
            futureAppt.setAppointmentTime(LocalTime.of(9, 0));
            futureAppt.setVisitType("NEW_VISIT");
            futureAppt.setStatus("REGISTERED");
            appointmentRepository.saveAndFlush(futureAppt);

            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPatients").value(0))
                    .andExpect(jsonPath("$.totalRegisteredPatients").value(1));
        }

        @Test
        @DisplayName("all status types are counted in totalPatients")
        void getTodayDashboard_AllStatuses_AllCountedInTotal() throws Exception {
            Patient p1 = createPatient("Registered P", "+91-7000000001");
            Patient p2 = createPatient("InProgress P", "+91-7000000002");
            Patient p3 = createPatient("Completed P", "+91-7000000003");

            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "IN_PROGRESS", LocalDate.now(), LocalTime.of(10, 0));
            createAppointment(p3, "NEW_VISIT", "COMPLETED", LocalDate.now(), LocalTime.of(11, 0));

            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPatients").value(3))
                    .andExpect(jsonPath("$.completedPatients").value(1));
        }

        @Test
        @DisplayName("returns 403 when JWT has no role claim (e.g. refresh token used as access token)")
        void getTodayDashboard_RefreshTokenUsedAsAccess_Returns403() throws Exception {
            // A refresh token is a valid JWT but lacks the role and hospitalId claims.
            // The security filter chain rejects it with 403 (no matching role authority).
            User user = userRepository.findAll().stream()
                    .filter(u -> u.getHospitalId().equals(hospitalId))
                    .findFirst().orElseThrow();
            String refreshToken = jwtService.generateRefreshToken(user.getUserId(), user.getUsername());

            mockMvc.perform(get(DASHBOARD_URL)
                            .header("Authorization", "Bearer " + refreshToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    // =======================================================================
    // GET /api/v1/patients/search — default (no criteria)
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/search — default (no criteria)")
    class DefaultTodayPatients {

        @Test
        @DisplayName("returns today's patients when no search criteria are provided")
        void getPatients_NoCriteria_ReturnsTodayPatients() throws Exception {
            Patient p = createPatient("Today Patient", "+91-9000000001");
            createAppointment(p, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            Patient p2 = createPatient("Yesterday Patient", "+91-9000000002");
            createAppointment(p2, "NEW_VISIT", "COMPLETED", LocalDate.now().minusDays(1), LocalTime.of(10, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(1))
                    .andExpect(jsonPath("$.patients[0].patientName").value("Today Patient"));
        }

        @Test
        @DisplayName("returns empty list when no patients have appointments today")
        void getPatients_NoCriteria_NoTodayAppointments_ReturnsEmptyList() throws Exception {
            Patient p = createPatient("Tomorrow Patient", "+91-9000000004");
            createAppointment(p, "NEW_VISIT", "REGISTERED", LocalDate.now().plusDays(1), LocalTime.of(9, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(0))
                    .andExpect(jsonPath("$.totalPatients").value(0));
        }

        @Test
        @DisplayName("default pagination uses page=0 and size=10")
        void getPatients_NoCriteria_DefaultPagination() throws Exception {
            Patient p = createPatient("Paged Patient", "+91-9000000005");
            createAppointment(p, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentPage").value(0))
                    .andExpect(jsonPath("$.pageSize").value(10));
        }
    }

    // =======================================================================
    // Validation
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/search — validation")
    class SearchValidation {

        @Test
        @DisplayName("returns 409 when fromDate is provided without toDate")
        void getPatients_OnlyFromDate_Returns409() throws Exception {
            mockMvc.perform(get(SEARCH_URL)
                            .param("fromDate", LocalDate.now().toString())
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when toDate is provided without fromDate")
        void getPatients_OnlyToDate_Returns409() throws Exception {
            mockMvc.perform(get(SEARCH_URL)
                            .param("toDate", LocalDate.now().toString())
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when fromDate is after toDate")
        void getPatients_FromDateAfterToDate_Returns409() throws Exception {
            mockMvc.perform(get(SEARCH_URL)
                            .param("fromDate", LocalDate.now().plusDays(5).toString())
                            .param("toDate", LocalDate.now().toString())
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when page size exceeds 100")
        void getPatients_PageSizeTooLarge_Returns409() throws Exception {
            mockMvc.perform(get(SEARCH_URL)
                            .param("size", "101")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when page size is 0")
        void getPatients_PageSizeZero_Returns409() throws Exception {
            mockMvc.perform(get(SEARCH_URL)
                            .param("size", "0")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when page number is negative")
        void getPatients_NegativePageNumber_Returns409() throws Exception {
            mockMvc.perform(get(SEARCH_URL)
                            .param("page", "-1")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }
    }

    // =======================================================================
    // Search by date range
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/search — by date range")
    class SearchByDateRange {

        @Test
        @DisplayName("returns patients with appointments in the date range")
        void getPatients_ByDateRange_ReturnsMatches() throws Exception {
            LocalDate today = LocalDate.now();
            Patient p1 = createPatient("Range Patient 1", "+91-6000000001");
            Patient p2 = createPatient("Range Patient 2", "+91-6000000002");
            Patient p3 = createPatient("Out Of Range", "+91-6000000003");

            createAppointment(p1, "NEW_VISIT", "REGISTERED", today, LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "COMPLETED", today.plusDays(2), LocalTime.of(10, 0));
            createAppointment(p3, "NEW_VISIT", "REGISTERED", today.plusDays(10), LocalTime.of(11, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("fromDate", today.toString())
                            .param("toDate", today.plusDays(3).toString())
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(2));
        }

        @Test
        @DisplayName("same fromDate and toDate fetches data for that single date")
        void getPatients_SameDateRange_ReturnsSingleDayResults() throws Exception {
            LocalDate today = LocalDate.now();
            Patient p = createPatient("Same Day Patient", "+91-6100000001");
            createAppointment(p, "NEW_VISIT", "REGISTERED", today, LocalTime.of(9, 0));

            Patient p2 = createPatient("Other Day Patient", "+91-6100000002");
            createAppointment(p2, "NEW_VISIT", "REGISTERED", today.plusDays(1), LocalTime.of(10, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("fromDate", today.toString())
                            .param("toDate", today.toString())
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(1))
                    .andExpect(jsonPath("$.patients[0].patientName").value("Same Day Patient"));
        }

        @Test
        @DisplayName("fromDate equal to toDate works as single-date query")
        void getPatients_FromDateEqualsToDate_FetchesSingleDate() throws Exception {
            LocalDate specificDate = LocalDate.now().plusDays(3);
            Patient p = createPatient("Specific Date Patient", "+91-6200000001");
            createAppointment(p, "NEW_VISIT", "REGISTERED", specificDate, LocalTime.of(14, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("fromDate", specificDate.toString())
                            .param("toDate", specificDate.toString())
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(1))
                    .andExpect(jsonPath("$.patients[0].patientName").value("Specific Date Patient"));
        }
    }

    // =======================================================================
    // Pagination
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/search — pagination")
    class SearchPagination {

        @Test
        @DisplayName("custom page and size are respected")
        void getPatients_CustomPageSize_ReturnsCorrectPage() throws Exception {
            LocalDate today = LocalDate.now();
            for (int i = 1; i <= 5; i++) {
                Patient p = createPatient("Page Patient " + i, "+91-700000000" + i);
                createAppointment(p, "NEW_VISIT", "REGISTERED", today, LocalTime.of(8 + i, 0));
            }

            mockMvc.perform(get(SEARCH_URL)
                            .param("size", "2")
                            .param("page", "0")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(2))
                    .andExpect(jsonPath("$.currentPage").value(0))
                    .andExpect(jsonPath("$.pageSize").value(2))
                    .andExpect(jsonPath("$.totalPages").value(3))
                    .andExpect(jsonPath("$.totalPatients").value(5));
        }

        @Test
        @DisplayName("second page returns remaining results")
        void getPatients_SecondPage_ReturnsRemaining() throws Exception {
            LocalDate today = LocalDate.now();
            for (int i = 1; i <= 3; i++) {
                Patient p = createPatient("Second Page Patient " + i, "+91-710000000" + i);
                createAppointment(p, "NEW_VISIT", "REGISTERED", today, LocalTime.of(8 + i, 0));
            }

            mockMvc.perform(get(SEARCH_URL)
                            .param("size", "2")
                            .param("page", "1")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(1))
                    .andExpect(jsonPath("$.currentPage").value(1))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.totalPatients").value(3));
        }

        @Test
        @DisplayName("empty page beyond total returns empty patients list")
        void getPatients_BeyondLastPage_ReturnsEmpty() throws Exception {
            Patient p = createPatient("Only Patient", "+91-7200000001");
            createAppointment(p, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("size", "10")
                            .param("page", "5")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(0))
                    .andExpect(jsonPath("$.totalPatients").value(1));
        }
    }

    // =======================================================================
    // Search by name and phone number
    // GET /api/v1/patients/search/by-name-phone?name=...&phone=...
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/search/by-name-phone — search by name and phone")
    class SearchByNameAndPhone {

        private static final String SEARCH_BY_NAME_PHONE_URL =
                ApiConstants.PATIENTS + ApiConstants.PATIENT_SEARCH_BY_NAME_PHONE;

        @Test
        @DisplayName("returns patients matching partial name (case-insensitive)")
        void searchByNamePhone_ByName_ReturnsMatchingPatients() throws Exception {
            Patient p1 = createPatient("Rajesh Kumar", "+91-8000000001");
            Patient p2 = createPatient("Suresh Sharma", "+91-8000000002");
            Patient p3 = createPatient("Priya Singh", "+91-8000000003");
            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(10, 0));
            createAppointment(p3, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(11, 0));

            mockMvc.perform(get(SEARCH_BY_NAME_PHONE_URL)
                            .param("name", "rajesh")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].patientName").value("Rajesh Kumar"));
        }

        @Test
        @DisplayName("name search is case-insensitive")
        void searchByNamePhone_CaseInsensitive_ReturnsMatch() throws Exception {
            Patient p = createPatient("Anita Desai", "+91-8100000001");
            createAppointment(p, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            mockMvc.perform(get(SEARCH_BY_NAME_PHONE_URL)
                            .param("name", "ANITA")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].patientName").value("Anita Desai"));
        }

        @Test
        @DisplayName("returns patients matching partial phone number")
        void searchByNamePhone_ByPhone_ReturnsMatchingPatients() throws Exception {
            Patient p1 = createPatient("Phone Patient 1", "+91-8200000001");
            Patient p2 = createPatient("Phone Patient 2", "+91-8200000002");
            Patient p3 = createPatient("Phone Patient 3", "+91-8300000003");
            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(10, 0));
            createAppointment(p3, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(11, 0));

            mockMvc.perform(get(SEARCH_BY_NAME_PHONE_URL)
                            .param("phoneNumber", "820000000")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("combines name and phone filters (AND logic)")
        void searchByNamePhone_ByNameAndPhone_ReturnsIntersection() throws Exception {
            Patient p1 = createPatient("Rahul Verma", "+91-8400000001");
            Patient p2 = createPatient("Rahul Gupta", "+91-8500000002");
            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(10, 0));

            mockMvc.perform(get(SEARCH_BY_NAME_PHONE_URL)
                            .param("name", "Rahul")
                            .param("phoneNumber", "840000")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].patientName").value("Rahul Verma"));
        }

        @Test
        @DisplayName("returns empty when name matches no patients")
        void searchByNamePhone_NonExistingName_ReturnsEmpty() throws Exception {
            Patient p = createPatient("Existing Patient", "+91-8700000001");
            createAppointment(p, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            mockMvc.perform(get(SEARCH_BY_NAME_PHONE_URL)
                            .param("name", "NonExistent")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("returns empty when phone matches no patients")
        void searchByNamePhone_NonExistingPhone_ReturnsEmpty() throws Exception {
            Patient p = createPatient("Phone Search Patient", "+91-8800000001");
            createAppointment(p, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            mockMvc.perform(get(SEARCH_BY_NAME_PHONE_URL)
                            .param("phoneNumber", "9999999999")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("searches across all appointment dates")
        void searchByNamePhone_ByName_SearchesAllDates() throws Exception {
            LocalDate today = LocalDate.now();
            Patient p = createPatient("AllDates Patient", "+91-8900000001");
            createAppointment(p, "NEW_VISIT", "COMPLETED", today.minusDays(5), LocalTime.of(9, 0));
            createAppointment(p, "NEW_VISIT", "REGISTERED", today.plusDays(3), LocalTime.of(10, 0));

            mockMvc.perform(get(SEARCH_BY_NAME_PHONE_URL)
                            .param("name", "AllDates")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("respects tenant isolation")
        void searchByNamePhone_MultiTenant_ReturnsOnlyOwnHospitalData() throws Exception {
            Patient ownPatient = createPatient("Shared Name Patient", "+91-8910000001");
            createAppointment(ownPatient, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            Hospital otherHospital = new Hospital();
            otherHospital.setName("Other Hospital");
            otherHospital.setSubdomain("other-name-" + UUID.randomUUID().toString().substring(0, 8));
            otherHospital.setAddress("456 Other Street");
            otherHospital.setContactEmail("other@name.com");
            otherHospital.setContactPhone("+91-9999999999");
            otherHospital.setActive(true);
            otherHospital = hospitalRepository.saveAndFlush(otherHospital);

            Patient otherPatient = new Patient();
            otherPatient.setHospital(otherHospital);
            otherPatient.setFullName("Shared Name Patient");
            otherPatient.setMobileNumber("+91-8910000002");
            otherPatient.setAge(40);
            otherPatient.setGender("FEMALE");
            otherPatient = patientRepository.saveAndFlush(otherPatient);

            Appointment otherAppt = new Appointment();
            otherAppt.setHospitalId(otherHospital.getHospitalId());
            otherAppt.setPatient(otherPatient);
            otherAppt.setAppointmentDate(LocalDate.now());
            otherAppt.setAppointmentTime(LocalTime.of(11, 0));
            otherAppt.setVisitType("NEW_VISIT");
            otherAppt.setStatus("REGISTERED");
            appointmentRepository.saveAndFlush(otherAppt);

            mockMvc.perform(get(SEARCH_BY_NAME_PHONE_URL)
                            .param("name", "Shared Name")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].mobileNumber").value("+91-8910000001"));
        }

        @Test
        @DisplayName("returns 409 when neither name nor phone is provided")
        void searchByNamePhone_NoParams_Returns409() throws Exception {
            mockMvc.perform(get(SEARCH_BY_NAME_PHONE_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("response contains all patient and appointment fields")
        void searchByNamePhone_ValidRequest_ContainsAllFields() throws Exception {
            Patient p = createPatient("Fields Patient", "+91-8920000001");
            createAppointment(p, "NEW_VISIT", "COMPLETED", LocalDate.now(), LocalTime.of(14, 30));

            mockMvc.perform(get(SEARCH_BY_NAME_PHONE_URL)
                            .param("name", "Fields")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].patientName").value("Fields Patient"))
                    .andExpect(jsonPath("$[0].mobileNumber").value("+91-8920000001"))
                    .andExpect(jsonPath("$[0].age").value(35))
                    .andExpect(jsonPath("$[0].gender").value("MALE"))
                    .andExpect(jsonPath("$[0].email").exists())
                    .andExpect(jsonPath("$[0].dateOfBirth").exists())
                    .andExpect(jsonPath("$[0].address").exists())
                    .andExpect(jsonPath("$[0].visitType").value("NEW_VISIT"))
                    .andExpect(jsonPath("$[0].appointmentDate").exists())
                    .andExpect(jsonPath("$[0].appointmentTime").exists())
                    .andExpect(jsonPath("$[0].appointmentStatus").value("COMPLETED"));
        }
    }

    // =======================================================================
    // Tenant isolation
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/search — tenant isolation")
    class SearchTenantIsolation {

        @Test
        @DisplayName("search only returns data for the authenticated user's hospital")
        void getPatients_MultiTenant_ReturnsOnlyOwnHospitalData() throws Exception {
            Patient ownPatient = createPatient("Own Hospital Patient", "+91-4000000001");
            createAppointment(ownPatient, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            Hospital otherHospital = new Hospital();
            otherHospital.setName("Other Hospital");
            otherHospital.setSubdomain("other-search-" + UUID.randomUUID().toString().substring(0, 8));
            otherHospital.setAddress("456 Other Street");
            otherHospital.setContactEmail("other@search.com");
            otherHospital.setContactPhone("+91-9999999999");
            otherHospital.setActive(true);
            otherHospital = hospitalRepository.saveAndFlush(otherHospital);

            Patient otherPatient = new Patient();
            otherPatient.setHospital(otherHospital);
            otherPatient.setFullName("Other Hospital Patient");
            otherPatient.setMobileNumber("+91-4000000002");
            otherPatient.setAge(40);
            otherPatient.setGender("FEMALE");
            otherPatient.setAge(40);
            otherPatient.setGender("FEMALE");
            otherPatient = patientRepository.saveAndFlush(otherPatient);

            Appointment otherAppt = new Appointment();
            otherAppt.setHospitalId(otherHospital.getHospitalId());
            otherAppt.setPatient(otherPatient);
            otherAppt.setAppointmentDate(LocalDate.now());
            otherAppt.setAppointmentTime(LocalTime.of(11, 0));
            otherAppt.setVisitType("NEW_VISIT");
            otherAppt.setStatus("REGISTERED");
            appointmentRepository.saveAndFlush(otherAppt);

            mockMvc.perform(get(SEARCH_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(1))
                    .andExpect(jsonPath("$.patients[0].mobileNumber").value("+91-4000000001"));
        }
    }

    // =======================================================================
    // Response format — counts and full patient details
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/search — response format")
    class SearchResponseFormat {

        @Test
        @DisplayName("response contains summary counts, pagination, and patients array")
        void getPatients_ValidRequest_ReturnsWrapperWithCountsAndPagination() throws Exception {
            Patient p1 = createPatient("Format Patient 1", "+91-3000000001");
            Patient p2 = createPatient("Format Patient 2", "+91-3000000002");
            Patient p3 = createPatient("Format Patient 3", "+91-3000000003");
            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "COMPLETED", LocalDate.now(), LocalTime.of(10, 0));
            createAppointment(p3, "NEW_VISIT", "IN_PROGRESS", LocalDate.now(), LocalTime.of(11, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPatients").value(3))
                    .andExpect(jsonPath("$.newPatients").value(3))
                    .andExpect(jsonPath("$.followUpPatients").value(0))
                    .andExpect(jsonPath("$.completedPatients").value(1))
                    .andExpect(jsonPath("$.currentPage").value(0))
                    .andExpect(jsonPath("$.pageSize").value(10))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.patients").isArray())
                    .andExpect(jsonPath("$.patients.length()").value(3));
        }

        @Test
        @DisplayName("each result contains all patient and appointment fields")
        void getPatients_ValidRequest_ResultContainsAllFields() throws Exception {
            Patient p = createPatient("Fields Patient", "+91-3100000001");
            createAppointment(p, "NEW_VISIT", "COMPLETED", LocalDate.now(), LocalTime.of(14, 30));

            mockMvc.perform(get(SEARCH_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients[0].appointmentId").exists())
                    .andExpect(jsonPath("$.patients[0].patientId").exists())
                    .andExpect(jsonPath("$.patients[0].patientName").value("Fields Patient"))
                    .andExpect(jsonPath("$.patients[0].mobileNumber").value("+91-3100000001"))
                    .andExpect(jsonPath("$.patients[0].age").value(35))
                    .andExpect(jsonPath("$.patients[0].gender").value("MALE"))
                    .andExpect(jsonPath("$.patients[0].email").exists())
                    .andExpect(jsonPath("$.patients[0].dateOfBirth").exists())
                    .andExpect(jsonPath("$.patients[0].address").exists())
                    .andExpect(jsonPath("$.patients[0].visitType").value("NEW_VISIT"))
                    .andExpect(jsonPath("$.patients[0].appointmentDate").exists())
                    .andExpect(jsonPath("$.patients[0].appointmentTime").exists())
                    .andExpect(jsonPath("$.patients[0].appointmentStatus").value("COMPLETED"));
        }
    }

    // =======================================================================
    // Filter by patientStatus and visitType
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/by-dates — filter by patientStatus and visitType")
    class FilterByStatusAndType {

        @Test
        @DisplayName("no patientStatus or visitType returns all patients for the date")
        void getPatients_NoFilters_ReturnsAll() throws Exception {
            LocalDate today = LocalDate.now();
            Patient p1 = createPatient("All Filter P1", "+91-9100000001");
            Patient p2 = createPatient("All Filter P2", "+91-9100000002");
            Patient p3 = createPatient("All Filter P3", "+91-9100000003");
            createAppointment(p1, "NEW_VISIT", "REGISTERED", today, LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "COMPLETED", today, LocalTime.of(10, 0));
            createAppointment(p3, "NEW_VISIT", "IN_PROGRESS", today, LocalTime.of(11, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(3));
        }

        @Test
        @DisplayName("patientStatus=COMPLETED returns only completed appointments")
        void getPatients_StatusCompleted_ReturnsOnlyCompleted() throws Exception {
            LocalDate today = LocalDate.now();
            Patient p1 = createPatient("Completed P", "+91-9110000001");
            Patient p2 = createPatient("Registered P", "+91-9110000002");
            createAppointment(p1, "NEW_VISIT", "COMPLETED", today, LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "REGISTERED", today, LocalTime.of(10, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("patientStatus", "COMPLETED")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(1))
                    .andExpect(jsonPath("$.patients[0].patientName").value("Completed P"))
                    .andExpect(jsonPath("$.patients[0].appointmentStatus").value("COMPLETED"));
        }

        @Test
        @DisplayName("patientStatus=REGISTERED returns only registered appointments")
        void getPatients_StatusRegistered_ReturnsOnlyRegistered() throws Exception {
            LocalDate today = LocalDate.now();
            Patient p1 = createPatient("Reg P", "+91-9120000001");
            Patient p2 = createPatient("Comp P", "+91-9120000002");
            createAppointment(p1, "NEW_VISIT", "REGISTERED", today, LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "COMPLETED", today, LocalTime.of(10, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("patientStatus", "REGISTERED")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(1))
                    .andExpect(jsonPath("$.patients[0].patientName").value("Reg P"));
        }

        @Test
        @DisplayName("patientStatus=IN_PROGRESS returns only in-progress appointments")
        void getPatients_StatusInProgress_ReturnsOnlyInProgress() throws Exception {
            LocalDate today = LocalDate.now();
            Patient p1 = createPatient("InProg P", "+91-9130000001");
            Patient p2 = createPatient("Done P", "+91-9130000002");
            createAppointment(p1, "NEW_VISIT", "IN_PROGRESS", today, LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "COMPLETED", today, LocalTime.of(10, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("patientStatus", "IN_PROGRESS")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(1))
                    .andExpect(jsonPath("$.patients[0].patientName").value("InProg P"));
        }

        @Test
        @DisplayName("visitType=NEW_VISIT returns only new visit appointments")
        void getPatients_TypeNewVisit_ReturnsOnlyNewVisits() throws Exception {
            LocalDate today = LocalDate.now();
            Patient p1 = createPatient("New Visit P", "+91-9140000001");
            Patient p2 = createPatient("FollowUp P", "+91-9140000002");
            createAppointment(p1, "NEW_VISIT", "REGISTERED", today, LocalTime.of(9, 0));
            createAppointment(p2, "FOLLOW_UP", "REGISTERED", today, LocalTime.of(10, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("visitType", "NEW_VISIT")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(1))
                    .andExpect(jsonPath("$.patients[0].patientName").value("New Visit P"))
                    .andExpect(jsonPath("$.patients[0].visitType").value("NEW_VISIT"));
        }

        @Test
        @DisplayName("visitType=FOLLOW_UP returns only follow-up appointments")
        void getPatients_TypeFollowUp_ReturnsOnlyFollowUps() throws Exception {
            LocalDate today = LocalDate.now();
            Patient p1 = createPatient("FU P", "+91-9150000001");
            Patient p2 = createPatient("New P", "+91-9150000002");
            createAppointment(p1, "FOLLOW_UP", "REGISTERED", today, LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "REGISTERED", today, LocalTime.of(10, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("visitType", "FOLLOW_UP")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    // FOLLOW_UP creates parent appointment on same date-7days, so only 1 on today
                    .andExpect(jsonPath("$.patients.length()").value(1))
                    .andExpect(jsonPath("$.patients[0].patientName").value("FU P"))
                    .andExpect(jsonPath("$.patients[0].visitType").value("FOLLOW_UP"));
        }

        @Test
        @DisplayName("both patientStatus and visitType filter together (AND logic)")
        void getPatients_StatusAndType_CombinesFilters() throws Exception {
            LocalDate today = LocalDate.now();
            Patient p1 = createPatient("New Comp P", "+91-9160000001");
            Patient p2 = createPatient("New Reg P", "+91-9160000002");
            Patient p3 = createPatient("FU Comp P", "+91-9160000003");
            createAppointment(p1, "NEW_VISIT", "COMPLETED", today, LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "REGISTERED", today, LocalTime.of(10, 0));
            createAppointment(p3, "FOLLOW_UP", "COMPLETED", today, LocalTime.of(11, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("patientStatus", "COMPLETED")
                            .param("visitType", "NEW_VISIT")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(1))
                    .andExpect(jsonPath("$.patients[0].patientName").value("New Comp P"));
        }

        @Test
        @DisplayName("patientStatus filter works with date range")
        void getPatients_StatusWithDateRange_FiltersCorrectly() throws Exception {
            LocalDate today = LocalDate.now();
            Patient p1 = createPatient("Range Comp P", "+91-9170000001");
            Patient p2 = createPatient("Range Reg P", "+91-9170000002");
            Patient p3 = createPatient("OutOfRange P", "+91-9170000003");
            createAppointment(p1, "NEW_VISIT", "COMPLETED", today, LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "REGISTERED", today.plusDays(1), LocalTime.of(10, 0));
            createAppointment(p3, "NEW_VISIT", "COMPLETED", today.plusDays(10), LocalTime.of(11, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("fromDate", today.toString())
                            .param("toDate", today.plusDays(3).toString())
                            .param("patientStatus", "COMPLETED")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(1))
                    .andExpect(jsonPath("$.patients[0].patientName").value("Range Comp P"));
        }

        @Test
        @DisplayName("visitType filter works with date range")
        void getPatients_TypeWithDateRange_FiltersCorrectly() throws Exception {
            LocalDate today = LocalDate.now();
            Patient p1 = createPatient("Range New P", "+91-9180000001");
            Patient p2 = createPatient("Range FU P", "+91-9180000002");
            createAppointment(p1, "NEW_VISIT", "REGISTERED", today, LocalTime.of(9, 0));
            createAppointment(p2, "FOLLOW_UP", "REGISTERED", today, LocalTime.of(10, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("fromDate", today.toString())
                            .param("toDate", today.plusDays(1).toString())
                            .param("visitType", "NEW_VISIT")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(1))
                    .andExpect(jsonPath("$.patients[0].patientName").value("Range New P"));
        }
    }

    // =======================================================================
    // Soft delete patient and appointments
    // =======================================================================

    @Nested
    @DisplayName("DELETE /api/v1/patients/{patientId} — soft delete patient and appointments")
    class DeletePatient {

        private static final String DELETE_URL = ApiConstants.PATIENTS;

        @Test
        @DisplayName("returns 200 when patient and appointments are soft-deleted successfully")
        void deletePatient_ValidPatient_Returns200() throws Exception {
            Patient p = createPatient("Delete Me", "+91-5000000001");
            createAppointment(p, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            mockMvc.perform(delete(DELETE_URL + "/" + p.getPatientId())
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("soft-deleted patient is excluded from search results")
        void deletePatient_Deleted_ExcludedFromSearch() throws Exception {
            Patient p1 = createPatient("Active Patient", "+91-5100000001");
            Patient p2 = createPatient("Deleted Patient", "+91-5100000002");
            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(10, 0));

            // Soft-delete p2 and its appointments
            mockMvc.perform(delete(DELETE_URL + "/" + p2.getPatientId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // Search should only return active patient
            mockMvc.perform(get(SEARCH_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(1))
                    .andExpect(jsonPath("$.patients[0].patientName").value("Active Patient"))
                    .andExpect(jsonPath("$.totalPatients").value(1));
        }

        @Test
        @DisplayName("soft-deleted patient is excluded from name/phone search")
        void deletePatient_Deleted_ExcludedFromNamePhoneSearch() throws Exception {
            Patient p1 = createPatient("Visible Person", "+91-5200000001");
            Patient p2 = createPatient("Hidden Person", "+91-5200000002");
            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(10, 0));

            mockMvc.perform(delete(DELETE_URL + "/" + p2.getPatientId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get(ApiConstants.PATIENTS + ApiConstants.PATIENT_SEARCH_BY_NAME_PHONE)
                            .param("name", "Person")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].patientName").value("Visible Person"));
        }

        @Test
        @DisplayName("soft-deleted patient is excluded from dashboard counts")
        void deletePatient_Deleted_ExcludedFromDashboardCounts() throws Exception {
            Patient p1 = createPatient("Dashboard Active", "+91-5300000001");
            Patient p2 = createPatient("Dashboard Deleted", "+91-5300000002");
            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "COMPLETED", LocalDate.now(), LocalTime.of(10, 0));

            mockMvc.perform(delete(DELETE_URL + "/" + p2.getPatientId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get(SEARCH_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPatients").value(1))
                    .andExpect(jsonPath("$.completedPatients").value(0));
        }

        @Test
        @DisplayName("returns 409 when patient does not exist")
        void deletePatient_NotFound_Returns409() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(delete(DELETE_URL + "/" + nonExistentId)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when deleting patient from another hospital")
        void deletePatient_WrongTenant_Returns409() throws Exception {
            Hospital otherHospital = new Hospital();
            otherHospital.setName("Other Hospital");
            otherHospital.setSubdomain("other-del-" + UUID.randomUUID().toString().substring(0, 8));
            otherHospital.setAddress("456 Other Street");
            otherHospital.setContactEmail("other@del.com");
            otherHospital.setContactPhone("+91-9999999999");
            otherHospital.setActive(true);
            otherHospital = hospitalRepository.saveAndFlush(otherHospital);

            Patient otherPatient = new Patient();
            otherPatient.setHospital(otherHospital);
            otherPatient.setFullName("Other Patient");
            otherPatient.setMobileNumber("+91-5400000001");
            otherPatient.setAge(40);
            otherPatient.setGender("FEMALE");
            otherPatient = patientRepository.saveAndFlush(otherPatient);

            mockMvc.perform(delete(DELETE_URL + "/" + otherPatient.getPatientId())
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when patient is already deleted")
        void deletePatient_AlreadyDeleted_Returns409() throws Exception {
            Patient p = createPatient("Already Deleted", "+91-5500000001");
            createAppointment(p, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            // First delete — should succeed
            mockMvc.perform(delete(DELETE_URL + "/" + p.getPatientId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // Second delete — should fail
            mockMvc.perform(delete(DELETE_URL + "/" + p.getPatientId())
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }
    }

    // =======================================================================
    // Soft delete appointment
    // =======================================================================

    @Nested
    @DisplayName("DELETE /api/v1/appointments/{appointmentId} — soft delete appointment")
    class DeleteAppointment {

        private static final String DELETE_URL = ApiConstants.APPOINTMENTS;

        @Test
        @DisplayName("returns 200 when appointment is soft-deleted successfully")
        void deleteAppointment_Valid_Returns200() throws Exception {
            Patient p = createPatient("Appt Delete", "+91-6000000001");
            Appointment a = createAppointment(p, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            mockMvc.perform(delete(DELETE_URL + "/" + a.getAppointmentId())
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("soft-deleted appointment is excluded from search results")
        void deleteAppointment_Deleted_ExcludedFromSearch() throws Exception {
            Patient p1 = createPatient("Active Appt", "+91-6100000001");
            Patient p2 = createPatient("Deleted Appt", "+91-6100000002");
            Appointment a1 = createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));
            Appointment a2 = createAppointment(p2, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(10, 0));

            // Soft-delete a2
            mockMvc.perform(delete(DELETE_URL + "/" + a2.getAppointmentId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // Search should only return active appointment
            mockMvc.perform(get(SEARCH_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(1))
                    .andExpect(jsonPath("$.patients[0].patientName").value("Active Appt"))
                    .andExpect(jsonPath("$.totalPatients").value(1));
        }

        @Test
        @DisplayName("soft-deleted appointment is excluded from name/phone search")
        void deleteAppointment_Deleted_ExcludedFromNamePhoneSearch() throws Exception {
            Patient p1 = createPatient("Visible Appt", "+91-6200000001");
            Patient p2 = createPatient("Hidden Appt", "+91-6200000002");
            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));
            Appointment a2 = createAppointment(p2, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(10, 0));

            mockMvc.perform(delete(DELETE_URL + "/" + a2.getAppointmentId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get(ApiConstants.PATIENTS + ApiConstants.PATIENT_SEARCH_BY_NAME_PHONE)
                            .param("name", "Appt")
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].patientName").value("Visible Appt"));
        }

        @Test
        @DisplayName("soft-deleted appointment is excluded from dashboard counts")
        void deleteAppointment_Deleted_ExcludedFromDashboardCounts() throws Exception {
            Patient p1 = createPatient("Dash Active Appt", "+91-6300000001");
            Patient p2 = createPatient("Dash Deleted Appt", "+91-6300000002");
            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));
            Appointment a2 = createAppointment(p2, "NEW_VISIT", "COMPLETED", LocalDate.now(), LocalTime.of(10, 0));

            mockMvc.perform(delete(DELETE_URL + "/" + a2.getAppointmentId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get(SEARCH_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPatients").value(1))
                    .andExpect(jsonPath("$.completedPatients").value(0));
        }

        @Test
        @DisplayName("returns 409 when appointment does not exist")
        void deleteAppointment_NotFound_Returns409() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(delete(DELETE_URL + "/" + nonExistentId)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when deleting appointment from another hospital")
        void deleteAppointment_WrongTenant_Returns409() throws Exception {
            Hospital otherHospital = new Hospital();
            otherHospital.setName("Other Appt Hospital");
            otherHospital.setSubdomain("other-appt-" + UUID.randomUUID().toString().substring(0, 8));
            otherHospital.setAddress("789 Other Street");
            otherHospital.setContactEmail("other-appt@del.com");
            otherHospital.setContactPhone("+91-8888888888");
            otherHospital.setActive(true);
            otherHospital = hospitalRepository.saveAndFlush(otherHospital);

            Patient otherPatient = new Patient();
            otherPatient.setHospital(otherHospital);
            otherPatient.setFullName("Other Appt Patient");
            otherPatient.setMobileNumber("+91-6400000001");
            otherPatient.setAge(35);
            otherPatient.setGender("MALE");
            otherPatient = patientRepository.saveAndFlush(otherPatient);

            Appointment otherAppt = new Appointment();
            otherAppt.setHospitalId(otherHospital.getHospitalId());
            otherAppt.setPatient(otherPatient);
            otherAppt.setVisitType("NEW_VISIT");
            otherAppt.setStatus("REGISTERED");
            otherAppt.setAppointmentDate(LocalDate.now());
            otherAppt.setAppointmentTime(LocalTime.of(9, 0));
            otherAppt = appointmentRepository.saveAndFlush(otherAppt);

            mockMvc.perform(delete(DELETE_URL + "/" + otherAppt.getAppointmentId())
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when appointment is already deleted")
        void deleteAppointment_AlreadyDeleted_Returns409() throws Exception {
            Patient p = createPatient("Already Del Appt", "+91-6500000001");
            Appointment a = createAppointment(p, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            // First delete — should succeed
            mockMvc.perform(delete(DELETE_URL + "/" + a.getAppointmentId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // Second delete — should fail
            mockMvc.perform(delete(DELETE_URL + "/" + a.getAppointmentId())
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }
    }

    // =======================================================================
    // Context loads
    // =======================================================================

    @Test
    @DisplayName("application context loads")
    void contextLoads() {
        assertTrue(true);
    }
}
