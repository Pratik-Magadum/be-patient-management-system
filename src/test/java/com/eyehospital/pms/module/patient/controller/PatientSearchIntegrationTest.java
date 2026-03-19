package com.eyehospital.pms.module.patient.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.eyehospital.pms.BaseIntegrationTest;
import com.eyehospital.pms.common.constants.ApiConstants;
import com.eyehospital.pms.infrastructure.tenant.entity.Hospital;
import com.eyehospital.pms.infrastructure.tenant.repository.HospitalRepository;
import com.eyehospital.pms.module.appointment.entity.Appointment;
import com.eyehospital.pms.module.appointment.repository.AppointmentRepository;
import com.eyehospital.pms.module.patient.entity.Patient;
import com.eyehospital.pms.module.patient.repository.PatientRepository;

/**
 * Integration tests for {@code GET /api/v1/patients/search}.
 *
 * <p>Security filters are disabled ({@code addFilters = false} inherited from
 * {@link BaseIntegrationTest}) so these tests focus purely on the search
 * business logic, validation, and response format.</p>
 */
@DisplayName("PatientController — getPatients Integration Tests")
class PatientSearchIntegrationTest extends BaseIntegrationTest {

    private static final String SEARCH_URL =
            ApiConstants.PATIENTS + ApiConstants.PATIENT_SEARCH;

    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentRepository appointmentRepository;

    private UUID hospitalId;

    @BeforeEach
    void setUp() {
        Hospital hospital = new Hospital();
        hospital.setName("Search Test Hospital");
        hospital.setSubdomain("search-test-" + UUID.randomUUID().toString().substring(0, 8));
        hospital.setAddress("123 Search Street");
        hospital.setContactEmail("search@test.com");
        hospital.setContactPhone("+91-1234567890");
        hospital.setActive(true);
        hospital = hospitalRepository.saveAndFlush(hospital);
        hospitalId = hospital.getHospitalId();
    }

    // =======================================================================
    // Helpers
    // =======================================================================

    private Patient createPatient(String fullName, String mobile) {
        Patient patient = new Patient();
        patient.setHospital(hospitalRepository.getReferenceById(hospitalId));
        patient.setFullName(fullName);
        patient.setMobileNumber(mobile);
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
    // Default behaviour — no criteria returns today's patients
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/search — default (no criteria)")
    class DefaultTodayPatients {

        @Test
        @DisplayName("returns today's patients when no search criteria are provided")
        void getPatients_NoCriteria_ReturnsTodayPatients() throws Exception {
            Patient p = createPatient("Today Patient", "+91-9000000001");
            createAppointment(p, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            // Yesterday's appointment — should NOT be returned
            Patient p2 = createPatient("Yesterday Patient", "+91-9000000002");
            createAppointment(p2, "NEW_VISIT", "COMPLETED", LocalDate.now().minusDays(1), LocalTime.of(10, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].patientName").value("Today Patient"));
        }

        @Test
        @DisplayName("returns today's patients when all params are empty strings")
        void getPatients_EmptyStrings_ReturnsTodayPatients() throws Exception {
            Patient p = createPatient("Today Empty", "+91-9000000003");
            createAppointment(p, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("name", "")
                            .param("phone", "")
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].patientName").value("Today Empty"));
        }

        @Test
        @DisplayName("returns empty list when no patients have appointments today")
        void getPatients_NoCriteria_NoTodayAppointments_ReturnsEmptyList() throws Exception {
            Patient p = createPatient("Tomorrow Patient", "+91-9000000004");
            createAppointment(p, "NEW_VISIT", "REGISTERED", LocalDate.now().plusDays(1), LocalTime.of(9, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
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
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when toDate is provided without fromDate")
        void getPatients_OnlyToDate_Returns409() throws Exception {
            mockMvc.perform(get(SEARCH_URL)
                            .param("toDate", LocalDate.now().toString())
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when fromDate is after toDate")
        void getPatients_FromDateAfterToDate_Returns409() throws Exception {
            mockMvc.perform(get(SEARCH_URL)
                            .param("fromDate", LocalDate.now().plusDays(5).toString())
                            .param("toDate", LocalDate.now().toString())
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }
    }

    // =======================================================================
    // Search by name
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/search — by name")
    class SearchByName {

        @Test
        @DisplayName("returns matching patients when searching by name")
        void getPatients_ByName_ReturnsMatches() throws Exception {
            Patient p1 = createPatient("Rajesh Kumar", "+91-9000000001");
            Patient p2 = createPatient("Rajesh Sharma", "+91-9000000002");
            createPatient("Suresh Patel", "+91-9000000003");

            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "COMPLETED", LocalDate.now(), LocalTime.of(10, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("name", "Rajesh")
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].patientName").exists())
                    .andExpect(jsonPath("$[0].mobileNumber").exists())
                    .andExpect(jsonPath("$[0].visitType").exists())
                    .andExpect(jsonPath("$[0].appointmentTime").exists())
                    .andExpect(jsonPath("$[0].appointmentStatus").exists());
        }

        @Test
        @DisplayName("name search is case-insensitive")
        void getPatients_ByNameCaseInsensitive_ReturnsMatches() throws Exception {
            Patient p1 = createPatient("Rajesh Kumar", "+91-9100000001");
            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("name", "rajesh")
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].patientName").value("Rajesh Kumar"));
        }

        @Test
        @DisplayName("returns empty list when no patients match the name")
        void getPatients_ByNameNoMatch_ReturnsEmptyList() throws Exception {
            Patient p1 = createPatient("Rajesh Kumar", "+91-9200000001");
            createAppointment(p1, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("name", "Nonexistent")
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // =======================================================================
    // Search by phone number
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/search — by phone")
    class SearchByPhone {

        @Test
        @DisplayName("returns matching patients when searching by phone number")
        void getPatients_ByPhone_ReturnsMatches() throws Exception {
            Patient p1 = createPatient("Phone Patient", "+91-8000000001");
            createAppointment(p1, "NEW_VISIT", "IN_PROGRESS", LocalDate.now(), LocalTime.of(11, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("phone", "8000000001")
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].patientName").value("Phone Patient"))
                    .andExpect(jsonPath("$[0].mobileNumber").value("+91-8000000001"));
        }

        @Test
        @DisplayName("returns empty list when no patients match the phone")
        void getPatients_ByPhoneNoMatch_ReturnsEmptyList() throws Exception {
            mockMvc.perform(get(SEARCH_URL)
                            .param("phone", "0000000000")
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
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
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("same fromDate and toDate fetches data for that single date")
        void getPatients_SameDateRange_ReturnsSingleDayResults() throws Exception {
            LocalDate today = LocalDate.now();
            Patient p = createPatient("Same Day Patient", "+91-6100000001");
            createAppointment(p, "NEW_VISIT", "REGISTERED", today, LocalTime.of(9, 0));

            // Different day — should NOT be returned
            Patient p2 = createPatient("Other Day Patient", "+91-6100000002");
            createAppointment(p2, "NEW_VISIT", "REGISTERED", today.plusDays(1), LocalTime.of(10, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("fromDate", today.toString())
                            .param("toDate", today.toString())
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].patientName").value("Same Day Patient"));
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
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].patientName").value("Specific Date Patient"));
        }
    }

    // =======================================================================
    // Combined criteria
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/search — combined criteria")
    class SearchCombined {

        @Test
        @DisplayName("name + date range returns intersection of both criteria")
        void getPatients_NameAndDateRange_ReturnsIntersection() throws Exception {
            LocalDate today = LocalDate.now();
            Patient p1 = createPatient("Rajesh Today", "+91-5000000001");
            Patient p2 = createPatient("Rajesh Tomorrow", "+91-5000000002");

            createAppointment(p1, "NEW_VISIT", "REGISTERED", today, LocalTime.of(9, 0));
            createAppointment(p2, "NEW_VISIT", "REGISTERED", today.plusDays(1), LocalTime.of(10, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("name", "Rajesh")
                            .param("fromDate", today.toString())
                            .param("toDate", today.toString())
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].patientName").value("Rajesh Today"));
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

            // Create patient in a different hospital
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
            otherPatient.setFullName("Own Hospital Patient");
            otherPatient.setMobileNumber("+91-4000000002");
            otherPatient = patientRepository.saveAndFlush(otherPatient);

            Appointment otherAppt = new Appointment();
            otherAppt.setHospitalId(otherHospital.getHospitalId());
            otherAppt.setPatient(otherPatient);
            otherAppt.setAppointmentDate(LocalDate.now());
            otherAppt.setAppointmentTime(LocalTime.of(11, 0));
            otherAppt.setVisitType("NEW_VISIT");
            otherAppt.setStatus("REGISTERED");
            appointmentRepository.saveAndFlush(otherAppt);

            // Search by the common name — should only return own hospital data
            mockMvc.perform(get(SEARCH_URL)
                            .param("name", "Own Hospital Patient")
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].mobileNumber").value("+91-4000000001"));
        }
    }

    // =======================================================================
    // Response format
    // =======================================================================

    @Nested
    @DisplayName("GET /api/v1/patients/search — response format")
    class SearchResponseFormat {

        @Test
        @DisplayName("response is a plain JSON array (no ApiResponseDto envelope)")
        void getPatients_ValidRequest_ReturnsPlainArray() throws Exception {
            Patient p = createPatient("Format Patient", "+91-3000000001");
            createAppointment(p, "NEW_VISIT", "REGISTERED", LocalDate.now(), LocalTime.of(9, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .param("name", "Format")
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    // No ApiResponseDto envelope fields
                    .andExpect(jsonPath("$.status").doesNotExist())
                    .andExpect(jsonPath("$.message").doesNotExist())
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.timestamp").doesNotExist());
        }

        @Test
        @DisplayName("each result has patientName, mobileNumber, visitType, appointmentTime, appointmentStatus")
        void getPatients_ValidRequest_ResultContainsAllFields() throws Exception {
            Patient p = createPatient("Fields Patient", "+91-3100000001");
            createAppointment(p, "NEW_VISIT", "COMPLETED", LocalDate.now(), LocalTime.of(14, 30));

            mockMvc.perform(get(SEARCH_URL)
                            .param("name", "Fields")
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].patientName").value("Fields Patient"))
                    .andExpect(jsonPath("$[0].mobileNumber").value("+91-3100000001"))
                    .andExpect(jsonPath("$[0].visitType").value("NEW_VISIT"))
                    .andExpect(jsonPath("$[0].appointmentTime").exists())
                    .andExpect(jsonPath("$[0].appointmentStatus").value("COMPLETED"));
        }
    }
}
