package com.eyehospital.pms.module.appointment.controller;

import java.time.LocalDate;
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
 * Integration tests for {@code POST /api/v1/appointments/follow-up}.
 *
 * <p>Security filters are disabled ({@code addFilters = false} inherited from
 * {@link BaseIntegrationTest}) so these tests focus purely on the follow-up
 * registration business logic and validation.</p>
 */
@DisplayName("AppointmentController — Follow-Up Registration Integration Tests")@AutoConfigureMockMvc(addFilters = false)class FollowUpAppointmentIntegrationTest extends BaseIntegrationTest {

    private static final String FOLLOW_UP_URL =
            ApiConstants.APPOINTMENTS + ApiConstants.APPOINTMENT_FOLLOW_UP;

    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentRepository appointmentRepository;

    private UUID hospitalId;

    @BeforeEach
    void setUp() {
        Hospital hospital = new Hospital();
        hospital.setName("Follow-Up Test Hospital");
        hospital.setSubdomain("followup-test-" + UUID.randomUUID().toString().substring(0, 8));
        hospital.setAddress("123 Follow-Up Street");
        hospital.setContactEmail("followup@test.com");
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

    private Appointment createCompletedNewVisit(Patient patient, LocalDate date, LocalTime time) {
        Appointment appt = new Appointment();
        appt.setHospitalId(hospitalId);
        appt.setPatient(patient);
        appt.setAppointmentDate(date);
        appt.setAppointmentTime(time);
        appt.setVisitType("NEW_VISIT");
        appt.setStatus("COMPLETED");
        return appointmentRepository.saveAndFlush(appt);
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

    // =======================================================================
    // Successful follow-up registration
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

            // First follow-up (completed)
            Appointment firstFollowUp = new Appointment();
            firstFollowUp.setHospitalId(hospitalId);
            firstFollowUp.setPatient(patient);
            firstFollowUp.setAppointmentDate(LocalDate.now().minusDays(7));
            firstFollowUp.setAppointmentTime(LocalTime.of(10, 0));
            firstFollowUp.setVisitType("FOLLOW_UP");
            firstFollowUp.setStatus("COMPLETED");
            firstFollowUp.setParentAppointment(originalVisit);
            firstFollowUp = appointmentRepository.saveAndFlush(firstFollowUp);

            // Second follow-up from first follow-up
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
    // Validation
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
            // Create appointment in another hospital
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

            Appointment registeredAppt = new Appointment();
            registeredAppt.setHospitalId(hospitalId);
            registeredAppt.setPatient(patient);
            registeredAppt.setAppointmentDate(LocalDate.now().minusDays(1));
            registeredAppt.setAppointmentTime(LocalTime.of(9, 0));
            registeredAppt.setVisitType("NEW_VISIT");
            registeredAppt.setStatus("REGISTERED");
            registeredAppt = appointmentRepository.saveAndFlush(registeredAppt);

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

            // Soft-delete the patient
            patient.setDeleted(true);
            patient.setDeletedAt(java.time.LocalDateTime.now());
            patientRepository.saveAndFlush(patient);

            mockMvc.perform(post(FOLLOW_UP_URL)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(followUpRequestJson(parent.getAppointmentId(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), null)))
                    .andExpect(status().isConflict());
        }
    }

    // =======================================================================
    // Tenant isolation
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
}
