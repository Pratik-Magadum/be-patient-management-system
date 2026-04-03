package com.eyehospital.pms.module.appointment.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Integration tests for all {@code /api/v1/appointments} endpoints:
 * register, follow-up, and update-status.
 *
 * <p>Security filters are disabled ({@code addFilters = false} inherited from
 * {@link BaseIntegrationTest}) so these tests focus purely on
 * business logic and validation.</p>
 */
@DisplayName("AppointmentController — Integration Tests")
@AutoConfigureMockMvc(addFilters = false)
class AppointmentIntegrationTest extends BaseIntegrationTest {

    private static final String REGISTER_URL =
            ApiConstants.APPOINTMENTS + ApiConstants.APPOINTMENT_REGISTER;
    private static final String FOLLOW_UP_URL =
            ApiConstants.APPOINTMENTS + ApiConstants.APPOINTMENT_FOLLOW_UP;
    private static final String STATUS_URL =
            ApiConstants.APPOINTMENTS + "/%s/status";

    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentRepository appointmentRepository;

    private UUID hospitalId;

    @BeforeEach
    void setUp() {
        Hospital hospital = new Hospital();
        hospital.setName("Appointment Test Hospital");
        hospital.setSubdomain("appt-test-" + UUID.randomUUID().toString().substring(0, 8));
        hospital.setAddress("123 Test Street");
        hospital.setContactEmail("appt@test.com");
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

    private Appointment createAppointment(Patient patient, LocalDate date, LocalTime time,
                                          String visitType, String status, Appointment parent) {
        Appointment appt = new Appointment();
        appt.setHospitalId(hospitalId);
        appt.setPatient(patient);
        appt.setAppointmentDate(date);
        appt.setAppointmentTime(time);
        appt.setVisitType(visitType);
        appt.setStatus(status);
        if (parent != null) {
            appt.setParentAppointment(parent);
        }
        return appointmentRepository.saveAndFlush(appt);
    }

    private Appointment createCompletedNewVisit(Patient patient, LocalDate date, LocalTime time) {
        return createAppointment(patient, date, time, "NEW_VISIT", "COMPLETED", null);
    }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private String followUpRequestJson(UUID parentAppointmentId, LocalDate date, LocalTime time, String notes) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"parentAppointmentId\":\"").append(parentAppointmentId).append("\"");
        sb.append(",\"appointmentDate\":\"").append(date).append("\"");
        sb.append(",\"appointmentTime\":\"").append(time.format(TIME_FMT)).append("\"");
        if (notes != null) {
            sb.append(",\"notes\":\"").append(notes).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String registerRequestJson(String fullName, String mobileNumber, String email,
                                       Integer age, String gender, LocalDate dateOfBirth,
                                       String address,
                                       LocalDate appointmentDate, LocalTime appointmentTime, String notes) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"fullName\":\"").append(fullName).append("\"");
        sb.append(",\"mobileNumber\":\"").append(mobileNumber).append("\"");
        if (email != null) {
            sb.append(",\"email\":\"").append(email).append("\"");
        }
        sb.append(",\"age\":").append(age);
        if (gender != null) {
            sb.append(",\"gender\":\"").append(gender).append("\"");
        }
        if (dateOfBirth != null) {
            sb.append(",\"dateOfBirth\":\"").append(dateOfBirth).append("\"");
        }
        if (address != null) {
            sb.append(",\"address\":\"").append(address).append("\"");
        }
        sb.append(",\"appointmentDate\":\"").append(appointmentDate).append("\"");
        sb.append(",\"appointmentTime\":\"").append(appointmentTime.format(TIME_FMT)).append("\"");
        if (notes != null) {
            sb.append(",\"notes\":\"").append(notes).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    // =======================================================================
    // Register appointment — success
    // =======================================================================

    @Nested
    @DisplayName("POST /api/v1/appointments/register — success")
    class RegisterAppointmentSuccess {

        @Test
        @DisplayName("returns 201 with patient and appointment details")
        void register_ValidRequest_Returns201() throws Exception {
            LocalDate apptDate = LocalDate.now().plusDays(1);
            LocalTime apptTime = LocalTime.of(10, 30);

            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerRequestJson("Rajesh Kumar", "+91-9800000001",
                                    "rajesh@email.com", 35, "MALE", LocalDate.of(1990, 5, 15),
                                    "12 MG Road, Mumbai", apptDate, apptTime, "Eye checkup")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.appointmentId").exists())
                    .andExpect(jsonPath("$.patientId").exists())
                    .andExpect(jsonPath("$.patientNumber").exists())
                    .andExpect(jsonPath("$.patientName").value("Rajesh Kumar"))
                    .andExpect(jsonPath("$.mobileNumber").value("+91-9800000001"))
                    .andExpect(jsonPath("$.age").value(35))
                    .andExpect(jsonPath("$.gender").value("MALE"))
                    .andExpect(jsonPath("$.email").value("rajesh@email.com"))
                    .andExpect(jsonPath("$.dateOfBirth").value("1990-05-15"))
                    .andExpect(jsonPath("$.address").value("12 MG Road, Mumbai"))
                    .andExpect(jsonPath("$.visitType").value("NEW_VISIT"))
                    .andExpect(jsonPath("$.appointmentStatus").value("REGISTERED"))
                    .andExpect(jsonPath("$.appointmentDate").value(apptDate.toString()))
                    .andExpect(jsonPath("$.appointmentTime").value("10:30:00"))
                    .andExpect(jsonPath("$.notes").value("Eye checkup"));
        }

        @Test
        @DisplayName("optional fields can be omitted")
        void register_OnlyRequiredFields_Returns201() throws Exception {
            LocalDate apptDate = LocalDate.now().plusDays(2);
            LocalTime apptTime = LocalTime.of(14, 0);

            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerRequestJson("Minimal Patient", "+91-9800000002",
                                    null, 28, null, null,
                                    null, apptDate, apptTime, null)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.patientName").value("Minimal Patient"))
                    .andExpect(jsonPath("$.mobileNumber").value("+91-9800000002"))
                    .andExpect(jsonPath("$.email").isEmpty())
                    .andExpect(jsonPath("$.dateOfBirth").isEmpty())
                    .andExpect(jsonPath("$.address").isEmpty())
                    .andExpect(jsonPath("$.notes").isEmpty())
                    .andExpect(jsonPath("$.visitType").value("NEW_VISIT"))
                    .andExpect(jsonPath("$.appointmentStatus").value("REGISTERED"));
        }

        @Test
        @DisplayName("patient number is auto-generated")
        void register_PatientNumberGenerated() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerRequestJson("Auto Number Patient", "+91-9800000003",
                                    null, 30, null, null,
                                    null, LocalDate.now(), LocalTime.of(9, 0), null)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.patientNumber").isNotEmpty());
        }
    }

    // =======================================================================
    // Register appointment — validation
    // =======================================================================

    @Nested
    @DisplayName("POST /api/v1/appointments/register — validation")
    class RegisterAppointmentValidation {

        @Test
        @DisplayName("returns 400 when fullName is missing")
        void register_MissingFullName_Returns400() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mobileNumber\":\"+91-9800000010\","
                                    + "\"appointmentDate\":\"" + LocalDate.now().plusDays(1) + "\","
                                    + "\"appointmentTime\":\"10:00:00\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when fullName is blank")
        void register_BlankFullName_Returns400() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"  \",\"mobileNumber\":\"+91-9800000011\","
                                    + "\"appointmentDate\":\"" + LocalDate.now().plusDays(1) + "\","
                                    + "\"appointmentTime\":\"10:00:00\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when mobileNumber is missing")
        void register_MissingMobileNumber_Returns400() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Test Patient\","
                                    + "\"age\":30,"
                                    + "\"appointmentDate\":\"" + LocalDate.now().plusDays(1) + "\","
                                    + "\"appointmentTime\":\"10:00:00\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when age is missing")
        void register_MissingAge_Returns400() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Test Patient\","
                                    + "\"mobileNumber\":\"+91-9800000014\","
                                    + "\"appointmentDate\":\"" + LocalDate.now().plusDays(1) + "\","
                                    + "\"appointmentTime\":\"10:00:00\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when appointmentDate is missing")
        void register_MissingAppointmentDate_Returns400() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Test Patient\","
                                    + "\"mobileNumber\":\"+91-9800000012\","
                                    + "\"appointmentTime\":\"10:00:00\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when appointmentTime is missing")
        void register_MissingAppointmentTime_Returns400() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Test Patient\","
                                    + "\"mobileNumber\":\"+91-9800000013\","
                                    + "\"appointmentDate\":\"" + LocalDate.now().plusDays(1) + "\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when request body is empty")
        void register_EmptyBody_Returns400() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when request body is malformed JSON")
        void register_MalformedJson_Returns400() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 409 when mobile number already exists in the hospital")
        void register_DuplicateMobileNumber_Returns409() throws Exception {
            // First registration succeeds
            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerRequestJson("First Patient", "+91-9800000099",
                                    null, 30, null, null,
                                    null, LocalDate.now(), LocalTime.of(9, 0), null)))
                    .andExpect(status().isCreated());

            // Second registration with same mobile number fails
            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerRequestJson("Second Patient", "+91-9800000099",
                                    null, 25, null, null,
                                    null, LocalDate.now().plusDays(1), LocalTime.of(10, 0), null)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("A patient with this mobile number already exists in this hospital"));
        }
    }

    // =======================================================================
    // Register appointment — tenant isolation
    // =======================================================================

    @Nested
    @DisplayName("POST /api/v1/appointments/register — tenant isolation")
    class RegisterAppointmentTenantIsolation {

        @Test
        @DisplayName("appointment is created with correct hospitalId and does not appear in other hospital")
        void register_TenantIsolation() throws Exception {
            // Create in hospital 1
            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerRequestJson("Tenant Patient", "+91-9800000020",
                                    null, 45, null, null,
                                    null, LocalDate.now(), LocalTime.of(9, 0), null)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.patientName").value("Tenant Patient"));

            // Create another hospital
            Hospital otherHospital = new Hospital();
            otherHospital.setName("Other Reg Hospital");
            otherHospital.setSubdomain("other-reg-" + UUID.randomUUID().toString().substring(0, 8));
            otherHospital.setAddress("456 Other Street");
            otherHospital.setContactEmail("other-reg@test.com");
            otherHospital.setContactPhone("+91-7777777777");
            otherHospital.setActive(true);
            otherHospital = hospitalRepository.saveAndFlush(otherHospital);

            // Verify count: other hospital should have 0 appointments
            long count = appointmentRepository
                    .countByHospitalIdAndAppointmentDateAndDeletedFalse(otherHospital.getHospitalId(), LocalDate.now());
            assert count == 0 : "Other hospital should have 0 appointments, but found " + count;
        }

        @Test
        @DisplayName("same mobile number can be used in different hospitals")
        void register_SameMobileDifferentHospital_Returns201() throws Exception {
            // Register in hospital 1
            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerRequestJson("Hospital1 Patient", "+91-9800000088",
                                    null, 40, null, null,
                                    null, LocalDate.now(), LocalTime.of(9, 0), null)))
                    .andExpect(status().isCreated());

            // Create another hospital
            Hospital otherHospital = new Hospital();
            otherHospital.setName("Cross Tenant Hospital");
            otherHospital.setSubdomain("cross-tenant-" + UUID.randomUUID().toString().substring(0, 8));
            otherHospital.setAddress("789 Cross Street");
            otherHospital.setContactEmail("cross@test.com");
            otherHospital.setContactPhone("+91-6666666666");
            otherHospital.setActive(true);
            otherHospital = hospitalRepository.saveAndFlush(otherHospital);

            // Same mobile number in different hospital should succeed
            mockMvc.perform(post(REGISTER_URL)
                            .requestAttr("hospitalId", otherHospital.getHospitalId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerRequestJson("Hospital2 Patient", "+91-9800000088",
                                    null, 35, null, null,
                                    null, LocalDate.now(), LocalTime.of(10, 0), null)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.patientName").value("Hospital2 Patient"));
        }
    }

    // =======================================================================
    // Follow-up — success
    // =======================================================================

    @Nested
    @DisplayName("POST /api/v1/appointments/follow-up — success")
    class FollowUpSuccess {

        @Test
        @DisplayName("returns 201 with follow-up appointment details")
        void registerFollowUp_ValidRequest_Returns201() throws Exception {
            Patient patient = createPatient("Follow-Up Patient", "+91-9000000001");
            Appointment parent = createCompletedNewVisit(patient, LocalDate.now().minusDays(7), LocalTime.of(9, 0));

            LocalDate followUpDate = LocalDate.now().plusDays(3);
            LocalTime followUpTime = LocalTime.of(10, 30);

            mockMvc.perform(post(FOLLOW_UP_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(followUpRequestJson(parent.getAppointmentId(), followUpDate, followUpTime, "Follow-up checkup")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.appointmentId").exists())
                    .andExpect(jsonPath("$.patientId").value(patient.getPatientId().toString()))
                    .andExpect(jsonPath("$.patientName").value("Follow-Up Patient"))
                    .andExpect(jsonPath("$.patientNumber").exists())
                    .andExpect(jsonPath("$.mobileNumber").value("+91-9000000001"))
                    .andExpect(jsonPath("$.age").value(35))
                    .andExpect(jsonPath("$.gender").value("MALE"))
                    .andExpect(jsonPath("$.email").value("follow-up.patient@test.com"))
                    .andExpect(jsonPath("$.dateOfBirth").value("1990-05-15"))
                    .andExpect(jsonPath("$.address").value("123 Test Street"))
                    .andExpect(jsonPath("$.visitType").value("FOLLOW_UP"))
                    .andExpect(jsonPath("$.appointmentStatus").value("REGISTERED"))
                    .andExpect(jsonPath("$.appointmentDate").value(followUpDate.toString()))
                    .andExpect(jsonPath("$.appointmentTime").value("10:30:00"));
        }

        @Test
        @DisplayName("notes field is optional")
        void registerFollowUp_NoNotes_Returns201() throws Exception {
            Patient patient = createPatient("No Notes Patient", "+91-9000000002");
            Appointment parent = createCompletedNewVisit(patient, LocalDate.now().minusDays(5), LocalTime.of(9, 0));

            mockMvc.perform(post(FOLLOW_UP_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(followUpRequestJson(parent.getAppointmentId(), LocalDate.now().plusDays(1), LocalTime.of(14, 0), null)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.appointmentId").exists())
                    .andExpect(jsonPath("$.visitType").value("FOLLOW_UP"))
                    .andExpect(jsonPath("$.appointmentStatus").value("REGISTERED"))
                    .andExpect(jsonPath("$.patientName").value("No Notes Patient"));
        }

        @Test
        @DisplayName("follow-up can be created from another follow-up (chained)")
        void registerFollowUp_FromFollowUp_Returns201() throws Exception {
            Patient patient = createPatient("Chain Patient", "+91-9000000003");
            Appointment originalVisit = createCompletedNewVisit(patient, LocalDate.now().minusDays(14), LocalTime.of(9, 0));
            Appointment firstFollowUp = createAppointment(patient, LocalDate.now().minusDays(7),
                    LocalTime.of(10, 0), "FOLLOW_UP", "COMPLETED", originalVisit);

            mockMvc.perform(post(FOLLOW_UP_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(followUpRequestJson(firstFollowUp.getAppointmentId(), LocalDate.now().plusDays(2), LocalTime.of(11, 0), null)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.visitType").value("FOLLOW_UP"))
                    .andExpect(jsonPath("$.appointmentStatus").value("REGISTERED"));
        }
    }

    // =======================================================================
    // Follow-up — validation
    // =======================================================================

    @Nested
    @DisplayName("POST /api/v1/appointments/follow-up — validation")
    class FollowUpValidation {

        @Test
        @DisplayName("returns 400 when parentAppointmentId is missing")
        void registerFollowUp_MissingParentId_Returns400() throws Exception {
            mockMvc.perform(post(FOLLOW_UP_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"appointmentDate\":\"" + LocalDate.now().plusDays(1) + "\","
                                    + "\"appointmentTime\":\"10:00:00\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when appointmentDate is missing")
        void registerFollowUp_MissingDate_Returns400() throws Exception {
            Patient patient = createPatient("Missing Date P", "+91-9100000001");
            Appointment parent = createCompletedNewVisit(patient, LocalDate.now().minusDays(7), LocalTime.of(9, 0));

            mockMvc.perform(post(FOLLOW_UP_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"parentAppointmentId\":\"" + parent.getAppointmentId() + "\","
                                    + "\"appointmentTime\":\"10:00:00\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when appointmentTime is missing")
        void registerFollowUp_MissingTime_Returns400() throws Exception {
            Patient patient = createPatient("Missing Time P", "+91-9100000002");
            Appointment parent = createCompletedNewVisit(patient, LocalDate.now().minusDays(7), LocalTime.of(9, 0));

            mockMvc.perform(post(FOLLOW_UP_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"parentAppointmentId\":\"" + parent.getAppointmentId() + "\","
                                    + "\"appointmentDate\":\"" + LocalDate.now().plusDays(1) + "\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 409 when parent appointment does not exist")
        void registerFollowUp_ParentNotFound_Returns409() throws Exception {
            mockMvc.perform(post(FOLLOW_UP_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(followUpRequestJson(UUID.randomUUID(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), null)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when parent appointment belongs to another hospital")
        void registerFollowUp_ParentWrongTenant_Returns409() throws Exception {
            Hospital otherHospital = new Hospital();
            otherHospital.setName("Other Hospital");
            otherHospital.setSubdomain("other-fu-" + UUID.randomUUID().toString().substring(0, 8));
            otherHospital.setAddress("456 Other Street");
            otherHospital.setContactEmail("other@fu.com");
            otherHospital.setContactPhone("+91-9999999999");
            otherHospital.setActive(true);
            otherHospital = hospitalRepository.saveAndFlush(otherHospital);

            Patient otherPatient = new Patient();
            otherPatient.setHospital(otherHospital);
            otherPatient.setFullName("Other Tenant Patient");
            otherPatient.setMobileNumber("+91-9200000001");
            otherPatient.setAge(40);
            otherPatient.setGender("FEMALE");
            otherPatient = patientRepository.saveAndFlush(otherPatient);

            Appointment otherAppt = new Appointment();
            otherAppt.setHospitalId(otherHospital.getHospitalId());
            otherAppt.setPatient(otherPatient);
            otherAppt.setAppointmentDate(LocalDate.now().minusDays(7));
            otherAppt.setAppointmentTime(LocalTime.of(9, 0));
            otherAppt.setVisitType("NEW_VISIT");
            otherAppt.setStatus("COMPLETED");
            otherAppt = appointmentRepository.saveAndFlush(otherAppt);

            mockMvc.perform(post(FOLLOW_UP_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(followUpRequestJson(otherAppt.getAppointmentId(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), null)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when parent appointment is not COMPLETED")
        void registerFollowUp_ParentNotCompleted_Returns409() throws Exception {
            Patient patient = createPatient("Not Completed P", "+91-9200000002");
            Appointment registeredAppt = createAppointment(patient, LocalDate.now().minusDays(1),
                    LocalTime.of(9, 0), "NEW_VISIT", "REGISTERED", null);

            mockMvc.perform(post(FOLLOW_UP_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(followUpRequestJson(registeredAppt.getAppointmentId(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), null)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when parent appointment's patient is soft-deleted")
        void registerFollowUp_PatientDeleted_Returns409() throws Exception {
            Patient patient = createPatient("Deleted Patient", "+91-9200000003");
            Appointment parent = createCompletedNewVisit(patient, LocalDate.now().minusDays(7), LocalTime.of(9, 0));

            patient.setDeleted(true);
            patient.setDeletedAt(LocalDateTime.now());
            patientRepository.saveAndFlush(patient);

            mockMvc.perform(post(FOLLOW_UP_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(followUpRequestJson(parent.getAppointmentId(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), null)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when a follow-up already exists for the parent appointment")
        void registerFollowUp_AlreadyExists_Returns409() throws Exception {
            Patient patient = createPatient("Duplicate FU Patient", "+91-9200000004");
            Appointment parent = createCompletedNewVisit(patient, LocalDate.now().minusDays(7), LocalTime.of(9, 0));

            createAppointment(patient, LocalDate.now().plusDays(1),
                    LocalTime.of(10, 0), "FOLLOW_UP", "REGISTERED", parent);

            mockMvc.perform(post(FOLLOW_UP_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(followUpRequestJson(parent.getAppointmentId(), LocalDate.now().plusDays(2), LocalTime.of(11, 0), null)))
                    .andExpect(status().isConflict());
        }
    }

    // =======================================================================
    // Follow-up — tenant isolation
    // =======================================================================

    @Nested
    @DisplayName("POST /api/v1/appointments/follow-up — tenant isolation")
    class FollowUpTenantIsolation {

        @Test
        @DisplayName("follow-up appointment is created with correct hospitalId")
        void registerFollowUp_SetsCorrectHospitalId() throws Exception {
            Patient patient = createPatient("Tenant Patient", "+91-9300000001");
            Appointment parent = createCompletedNewVisit(patient, LocalDate.now().minusDays(7), LocalTime.of(9, 0));

            mockMvc.perform(post(FOLLOW_UP_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(followUpRequestJson(parent.getAppointmentId(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), null)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.patientId").value(patient.getPatientId().toString()));
        }
    }

    // =======================================================================
    // Update status — success
    // =======================================================================

    @Nested
    @DisplayName("PATCH /api/v1/appointments/{id}/status — success")
    class UpdateStatusSuccess {

        @Test
        @DisplayName("REGISTERED → IN_PROGRESS returns 200 with updated status")
        void registeredToInProgress_Returns200() throws Exception {
            Patient patient = createPatient("Status Patient", "+91-9400000001");
            Appointment appt = createAppointment(patient, LocalDate.now(), LocalTime.of(10, 0),
                    "NEW_VISIT", "REGISTERED", null);

            mockMvc.perform(patch(STATUS_URL.formatted(appt.getAppointmentId()))
                            .requestAttr("hospitalId", hospitalId.toString())
                            .param("status", "IN_PROGRESS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.appointmentId").value(appt.getAppointmentId().toString()))
                    .andExpect(jsonPath("$.appointmentStatus").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.patientName").value("Status Patient"));
        }

        @Test
        @DisplayName("IN_PROGRESS → COMPLETED returns 200 with updated status")
        void inProgressToCompleted_Returns200() throws Exception {
            Patient patient = createPatient("Status Patient", "+91-9400000002");
            Appointment appt = createAppointment(patient, LocalDate.now(), LocalTime.of(10, 0),
                    "NEW_VISIT", "IN_PROGRESS", null);

            mockMvc.perform(patch(STATUS_URL.formatted(appt.getAppointmentId()))
                            .requestAttr("hospitalId", hospitalId.toString())
                            .param("status", "COMPLETED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.appointmentStatus").value("COMPLETED"));
        }
    }

    // =======================================================================
    // Update status — validation
    // =======================================================================

    @Nested
    @DisplayName("PATCH /api/v1/appointments/{id}/status — validation")
    class UpdateStatusValidation {

        @Test
        @DisplayName("returns 400 when status param is missing")
        void missingStatus_Returns400() throws Exception {
            Patient patient = createPatient("Status Patient", "+91-9400000003");
            Appointment appt = createAppointment(patient, LocalDate.now(), LocalTime.of(10, 0),
                    "NEW_VISIT", "REGISTERED", null);

            mockMvc.perform(patch(STATUS_URL.formatted(appt.getAppointmentId()))
                            .requestAttr("hospitalId", hospitalId.toString()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when status is an invalid enum value")
        void invalidStatusValue_Returns400() throws Exception {
            Patient patient = createPatient("Status Patient", "+91-9400000004");
            Appointment appt = createAppointment(patient, LocalDate.now(), LocalTime.of(10, 0),
                    "NEW_VISIT", "REGISTERED", null);

            mockMvc.perform(patch(STATUS_URL.formatted(appt.getAppointmentId()))
                            .requestAttr("hospitalId", hospitalId.toString())
                            .param("status", "CANCELLED"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =======================================================================
    // Update status — business rules
    // =======================================================================

    @Nested
    @DisplayName("PATCH /api/v1/appointments/{id}/status — business rules")
    class UpdateStatusBusinessRules {

        @Test
        @DisplayName("returns 409 for invalid transition REGISTERED → COMPLETED")
        void registeredToCompleted_Returns409() throws Exception {
            Patient patient = createPatient("Status Patient", "+91-9400000005");
            Appointment appt = createAppointment(patient, LocalDate.now(), LocalTime.of(10, 0),
                    "NEW_VISIT", "REGISTERED", null);

            mockMvc.perform(patch(STATUS_URL.formatted(appt.getAppointmentId()))
                            .requestAttr("hospitalId", hospitalId.toString())
                            .param("status", "COMPLETED"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 for same status transition (no-op)")
        void sameStatus_Returns409() throws Exception {
            Patient patient = createPatient("Status Patient", "+91-9400000006");
            Appointment appt = createAppointment(patient, LocalDate.now(), LocalTime.of(10, 0),
                    "NEW_VISIT", "REGISTERED", null);

            mockMvc.perform(patch(STATUS_URL.formatted(appt.getAppointmentId()))
                            .requestAttr("hospitalId", hospitalId.toString())
                            .param("status", "REGISTERED"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 for backward transition COMPLETED → IN_PROGRESS")
        void completedToInProgress_Returns409() throws Exception {
            Patient patient = createPatient("Status Patient", "+91-9400000007");
            Appointment appt = createAppointment(patient, LocalDate.now(), LocalTime.of(10, 0),
                    "NEW_VISIT", "COMPLETED", null);

            mockMvc.perform(patch(STATUS_URL.formatted(appt.getAppointmentId()))
                            .requestAttr("hospitalId", hospitalId.toString())
                            .param("status", "IN_PROGRESS"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when appointment not found")
        void appointmentNotFound_Returns409() throws Exception {
            UUID fakeId = UUID.randomUUID();

            mockMvc.perform(patch(STATUS_URL.formatted(fakeId))
                            .requestAttr("hospitalId", hospitalId.toString())
                            .param("status", "IN_PROGRESS"))
                    .andExpect(status().isConflict());
        }
    }
}
