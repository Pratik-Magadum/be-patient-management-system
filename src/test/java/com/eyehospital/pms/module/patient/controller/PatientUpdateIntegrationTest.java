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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Integration tests for {@code PUT /api/v1/patients/{patientId}} — update patient endpoint.
 *
 * <p>Security filters are disabled ({@code addFilters = false} inherited from
 * {@link BaseIntegrationTest}) so these tests focus purely on
 * business logic and validation.</p>
 */
@DisplayName("PatientController — Update Patient Integration Tests")
@AutoConfigureMockMvc(addFilters = false)
class PatientUpdateIntegrationTest extends BaseIntegrationTest {

    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentRepository appointmentRepository;

    private UUID hospitalId;

    @BeforeEach
    void setUp() {
        Hospital hospital = new Hospital();
        hospital.setName("Update Test Hospital");
        hospital.setSubdomain("update-test-" + UUID.randomUUID().toString().substring(0, 8));
        hospital.setAddress("123 Update Street");
        hospital.setContactEmail("update@test.com");
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

    private Appointment createAppointment(Patient patient, String visitType, String status) {
        Appointment appt = new Appointment();
        appt.setHospitalId(hospitalId);
        appt.setPatient(patient);
        appt.setAppointmentDate(LocalDate.now());
        appt.setAppointmentTime(LocalTime.of(10, 0));
        appt.setVisitType(visitType);
        appt.setStatus(status);
        return appointmentRepository.saveAndFlush(appt);
    }

    private String updateRequestJson(String fullName, String mobile,
                                     String email, int age, String gender,
                                     String dateOfBirth, String address) {
        return """
                {
                    "fullName": "%s",
                    "mobileNumber": "%s",
                    "email": "%s",
                    "age": %d,
                    "gender": "%s",
                    "dateOfBirth": "%s",
                    "address": "%s",
                    "appointmentDate": "2026-04-01",
                    "appointmentTime": "10:30:00"
                }
                """.formatted(fullName, mobile, email, age, gender, dateOfBirth, address);
    }

    // =======================================================================
    // PUT /api/v1/patients/{patientId} — success scenarios
    // =======================================================================

    @Nested
    @DisplayName("Update Patient — Success")
    class UpdatePatientSuccess {

        @Test
        @DisplayName("returns 200 with updated patient details")
        void updatePatient_ValidRequest_ReturnsUpdatedDetails() throws Exception {
            Patient patient = createPatient("Original Name", "+91-9800000001");
            createAppointment(patient, "NEW_VISIT", "REGISTERED");

            String json = updateRequestJson(
                    "Updated Name", "+91-9800000001", "updated@test.com",
                    40, "FEMALE", "1985-03-20", "456 Updated Road");

            mockMvc.perform(put(ApiConstants.PATIENTS + "/" + patient.getPatientId())
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patientId").value(patient.getPatientId().toString()))
                    .andExpect(jsonPath("$.patientName").value("Updated Name"))
                    .andExpect(jsonPath("$.mobileNumber").value("+91-9800000001"))
                    .andExpect(jsonPath("$.email").value("updated@test.com"))
                    .andExpect(jsonPath("$.age").value(40))
                    .andExpect(jsonPath("$.gender").value("FEMALE"))
                    .andExpect(jsonPath("$.address").value("456 Updated Road"));
        }

        @Test
        @DisplayName("allows updating mobile number when no conflict")
        void updatePatient_NewMobileNumber_ReturnsUpdatedDetails() throws Exception {
            Patient patient = createPatient("Rajesh Kumar", "+91-9800000001");
            createAppointment(patient, "NEW_VISIT", "REGISTERED");

            String json = updateRequestJson(
                    "Rajesh Kumar", "+91-9999999999", "rajesh@test.com",
                    35, "MALE", "1990-05-15", "123 Test Street");

            mockMvc.perform(put(ApiConstants.PATIENTS + "/" + patient.getPatientId())
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mobileNumber").value("+91-9999999999"));
        }

        @Test
        @DisplayName("allows keeping the same mobile number (no false duplicate)")
        void updatePatient_SameMobileNumber_ReturnsUpdatedDetails() throws Exception {
            Patient patient = createPatient("Rajesh Kumar", "+91-9800000001");
            createAppointment(patient, "NEW_VISIT", "REGISTERED");

            String json = updateRequestJson(
                    "Rajesh Kumar Updated", "+91-9800000001", "rajesh@test.com",
                    36, "MALE", "1990-05-15", "Updated Address");

            mockMvc.perform(put(ApiConstants.PATIENTS + "/" + patient.getPatientId())
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patientName").value("Rajesh Kumar Updated"))
                    .andExpect(jsonPath("$.mobileNumber").value("+91-9800000001"));
        }
    }

    // =======================================================================
    // PUT /api/v1/patients/{patientId} — validation failures
    // =======================================================================

    @Nested
    @DisplayName("Update Patient — Validation")
    class UpdatePatientValidation {

        @Test
        @DisplayName("returns 400 when fullName is blank")
        void updatePatient_BlankFullName_Returns400() throws Exception {
            Patient patient = createPatient("Original Name", "+91-9800000001");

            String json = """
                    {
                        "fullName": "",
                        "mobileNumber": "+91-9800000001",
                        "age": 35,
                        "appointmentDate": "2026-04-01",
                        "appointmentTime": "10:30:00"
                    }
                    """;

            mockMvc.perform(put(ApiConstants.PATIENTS + "/" + patient.getPatientId())
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when age is null")
        void updatePatient_NullAge_Returns400() throws Exception {
            Patient patient = createPatient("Original Name", "+91-9800000001");

            String json = """
                    {
                        "fullName": "Updated Name",
                        "mobileNumber": "+91-9800000001",
                        "appointmentDate": "2026-04-01",
                        "appointmentTime": "10:30:00"
                    }
                    """;

            mockMvc.perform(put(ApiConstants.PATIENTS + "/" + patient.getPatientId())
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    // =======================================================================
    // PUT /api/v1/patients/{patientId} — business rule failures
    // =======================================================================

    @Nested
    @DisplayName("Update Patient — Business Rules")
    class UpdatePatientBusinessRules {

        @Test
        @DisplayName("returns 409 when patient not found")
        void updatePatient_PatientNotFound_Returns409() throws Exception {
            UUID fakeId = UUID.randomUUID();

            String json = updateRequestJson(
                    "Updated Name", "+91-9800000001", "updated@test.com",
                    35, "MALE", "1990-05-15", "123 Test Street");

            mockMvc.perform(put(ApiConstants.PATIENTS + "/" + fakeId)
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when mobile number already exists for another patient")
        void updatePatient_DuplicateMobile_Returns409() throws Exception {
            Patient patient1 = createPatient("Patient One", "+91-1111111111");
            Patient patient2 = createPatient("Patient Two", "+91-2222222222");

            String json = updateRequestJson(
                    "Patient Two Updated", "+91-1111111111", "p2@test.com",
                    30, "FEMALE", "1995-01-01", "789 Street");

            mockMvc.perform(put(ApiConstants.PATIENTS + "/" + patient2.getPatientId())
                            .requestAttr("hospitalId", hospitalId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isConflict());
        }
    }
}
