package com.eyehospital.pms.module.patient.controller;

import java.time.LocalDate;
import java.time.LocalTime;
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
import com.eyehospital.pms.module.appointment.entity.Appointment;
import com.eyehospital.pms.module.appointment.repository.AppointmentRepository;
import com.eyehospital.pms.module.patient.entity.Patient;
import com.eyehospital.pms.module.patient.repository.PatientRepository;

/**
 * Integration tests for {@code GET /api/v1/patients/dashboard/today}.
 *
 * <p>Filters are enabled so the full security filter chain (OAuth2 resource server,
 * {@code TenantContextFilter}) is exercised — mirroring production behaviour.</p>
 *
 * <p>Test data is created in {@code @BeforeEach} and rolled back after each test
 * thanks to {@code @Transactional} on {@link BaseIntegrationTest}.</p>
 *
 * <p>Naming convention: {@code methodName_StateUnderTest_ExpectedBehavior}</p>
 */
@DisplayName("PatientController — Dashboard Integration Tests")
@AutoConfigureMockMvc(addFilters = true)
class PatientControllerIntegrationTest extends BaseIntegrationTest {

    private static final String DASHBOARD_URL =
            ApiConstants.PATIENTS + ApiConstants.PATIENT_DASHBOARD_TODAY;

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
        return patientRepository.saveAndFlush(patient);
    }

    private Appointment createAppointment(Patient patient, String visitType,
                                          String status, LocalTime time) {
        Appointment appt = new Appointment();
        appt.setHospitalId(hospitalId);
        appt.setPatient(patient);
        appt.setAppointmentDate(LocalDate.now());
        appt.setAppointmentTime(time);
        appt.setVisitType(visitType);
        appt.setStatus(status);

        if ("FOLLOW_UP".equals(visitType)) {
            Appointment parent = new Appointment();
            parent.setHospitalId(hospitalId);
            parent.setPatient(patient);
            parent.setAppointmentDate(LocalDate.now().minusDays(7));
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

            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "COMPLETED", LocalTime.of(10, 0));

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

            createAppointment(p1, "FOLLOW_UP", "COMPLETED", LocalTime.of(14, 0));
            createAppointment(p2, "FOLLOW_UP", "IN_PROGRESS", LocalTime.of(14, 30));

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

            createAppointment(p1, "NEW_VISIT", "COMPLETED", LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "IN_PROGRESS", LocalTime.of(10, 0));
            createAppointment(p3, "FOLLOW_UP", "COMPLETED", LocalTime.of(14, 0));
            createAppointment(p4, "FOLLOW_UP", "REGISTERED", LocalTime.of(15, 0));

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

            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalTime.of(9, 0));

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
            createAppointment(ownPatient, "NEW_VISIT", "REGISTERED", LocalTime.of(9, 0));

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

            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "IN_PROGRESS", LocalTime.of(10, 0));
            createAppointment(p3, "NEW_VISIT", "COMPLETED", LocalTime.of(11, 0));

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
}
