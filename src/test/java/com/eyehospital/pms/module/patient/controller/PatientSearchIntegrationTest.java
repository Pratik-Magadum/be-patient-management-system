package com.eyehospital.pms.module.patient.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
 * business logic, validation, pagination, and response format.</p>
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

            Patient p2 = createPatient("Yesterday Patient", "+91-9000000002");
            createAppointment(p2, "NEW_VISIT", "COMPLETED", LocalDate.now().minusDays(1), LocalTime.of(10, 0));

            mockMvc.perform(get(SEARCH_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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

        @Test
        @DisplayName("returns 409 when page size exceeds 100")
        void getPatients_PageSizeTooLarge_Returns409() throws Exception {
            mockMvc.perform(get(SEARCH_URL)
                            .param("size", "101")
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when page size is 0")
        void getPatients_PageSizeZero_Returns409() throws Exception {
            mockMvc.perform(get(SEARCH_URL)
                            .param("size", "0")
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when page number is negative")
        void getPatients_NegativePageNumber_Returns409() throws Exception {
            mockMvc.perform(get(SEARCH_URL)
                            .param("page", "-1")
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patients.length()").value(0))
                    .andExpect(jsonPath("$.totalPatients").value(1));
        }
    }

    // =======================================================================
    // Search by name and phone number — NEW API
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
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].mobileNumber").value("+91-8910000001"));
        }

        @Test
        @DisplayName("returns 409 when neither name nor phone is provided")
        void searchByNamePhone_NoParams_Returns409() throws Exception {
            mockMvc.perform(get(SEARCH_BY_NAME_PHONE_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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
                            .requestAttr("hospitalId", hospitalId.toString())
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

            mockMvc.perform( get(SEARCH_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
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
}
