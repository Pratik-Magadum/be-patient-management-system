package com.eyehospital.pms.module.patient.controller;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
import com.eyehospital.pms.module.patient.entity.Patient;
import com.eyehospital.pms.module.patient.repository.PatientRepository;

/**
 * Integration tests for the Patient History API endpoint.
 *
 * <p>Endpoint under test: {@code GET /api/v1/patients/{patientId}/history}</p>
 *
 * <p>Filters are enabled so the full security filter chain (OAuth2, TenantContextFilter)
 * is exercised. Test data (appointments, consultations, prescriptions) is created via
 * JDBC to avoid needing JPA entities for consultation/prescription modules.</p>
 *
 * <p>Naming convention: {@code methodName_StateUnderTest_ExpectedBehavior}</p>
 */
@DisplayName("PatientHistory — Integration Tests")
@AutoConfigureMockMvc(addFilters = true)
class PatientHistoryIntegrationTest extends BaseIntegrationTest {

    private static final String HISTORY_URL =
            ApiConstants.PATIENTS + ApiConstants.PATIENT_HISTORY;

    @Autowired private UserRepository userRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID hospitalId;
    private UUID patientId;
    private String accessToken;

    @BeforeEach
    void setUp() {
        // 1. Create test hospital
        Hospital hospital = new Hospital();
        hospital.setName("History Test Hospital");
        hospital.setSubdomain("history-test-" + UUID.randomUUID().toString().substring(0, 8));
        hospital.setAddress("123 Test Street");
        hospital.setContactEmail("test@history.com");
        hospital.setContactPhone("+91-1234567890");
        hospital.setActive(true);
        hospital = hospitalRepository.saveAndFlush(hospital);
        hospitalId = hospital.getHospitalId();

        // 2. Create doctor user
        User doctor = new User();
        doctor.setHospitalId(hospitalId);
        doctor.setUsername("history-doc-" + UUID.randomUUID().toString().substring(0, 8));
        doctor.setPassword(passwordEncoder.encode("TestP@ss123"));
        doctor.setFullName("Dr. History Test");
        doctor.setEmail("history_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com");
        doctor.setRole("DOCTOR");
        doctor.setActive(true);
        doctor = userRepository.saveAndFlush(doctor);
        UUID doctorId = doctor.getUserId();

        // 3. Generate access token
        accessToken = jwtService.generateAccessToken(
                doctorId, doctor.getUsername(), hospitalId, doctor.getRole());

        // 4. Create test patient
        Patient patient = new Patient();
        patient.setHospital(hospital);
        patient.setFullName("History Patient");
        patient.setMobileNumber("+91-" + UUID.randomUUID().toString().substring(0, 10));
        patient.setAge(40);
        patient.setGender("MALE");
        patient.setEmail("history_patient_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com");
        patient = patientRepository.saveAndFlush(patient);
        patientId = patient.getPatientId();

        // 5. Create appointments + consultations + prescriptions via JDBC
        UUID appointmentId1 = UUID.randomUUID();
        UUID appointmentId2 = UUID.randomUUID();
        UUID consultationId1 = UUID.randomUUID();
        UUID consultationId2 = UUID.randomUUID();

        // Appointment 1 — completed NEW_VISIT, with consultation and prescription
        jdbcTemplate.update("""
                INSERT INTO appointments (appointment_id, hospital_id, patient_id, doctor_id,
                    appointment_date, appointment_time, status, visit_type, notes, created_by)
                VALUES (?, ?, ?, ?, '2024-06-15', '09:00:00', 'COMPLETED', 'NEW_VISIT',
                    'Routine eye checkup', ?)
                """, appointmentId1, hospitalId, patientId, doctorId, doctorId);

        jdbcTemplate.update("""
                INSERT INTO consultations (consultation_id, hospital_id, appointment_id, doctor_id,
                    diagnosis_notes, follow_up_date)
                VALUES (?, ?, ?, ?, 'Diabetic retinopathy screening - Normal', '2025-01-20')
                """, consultationId1, hospitalId, appointmentId1, doctorId);

        // Get a medicine_id from the seed data
        UUID medicineId = jdbcTemplate.queryForObject(
                "SELECT medicine_id FROM medicines WHERE name = 'Artificial Tears'", UUID.class);

        jdbcTemplate.update("""
                INSERT INTO prescriptions (hospital_id, consultation_id, medicine_id,
                    dosage, frequency, duration)
                VALUES (?, ?, ?, '1-2 drops', 'As needed', '30 days')
                """, hospitalId, consultationId1, medicineId);

        // Appointment 2 — completed FOLLOW_UP, with consultation but no prescription
        jdbcTemplate.update("""
                INSERT INTO appointments (appointment_id, hospital_id, patient_id, doctor_id,
                    appointment_date, appointment_time, status, visit_type, parent_appointment_id, notes, created_by)
                VALUES (?, ?, ?, ?, '2024-12-20', '10:30:00', 'COMPLETED', 'FOLLOW_UP', ?,
                    'Follow-up visit', ?)
                """, appointmentId2, hospitalId, patientId, doctorId, appointmentId1, doctorId);

        jdbcTemplate.update("""
                INSERT INTO consultations (consultation_id, hospital_id, appointment_id, doctor_id,
                    diagnosis_notes)
                VALUES (?, ?, ?, ?, 'Cataracts - early stage')
                """, consultationId2, hospitalId, appointmentId2, doctorId);

        // Appointment 3 — registered (no consultation yet), should still appear in history
        jdbcTemplate.update("""
                INSERT INTO appointments (appointment_id, hospital_id, patient_id, doctor_id,
                    appointment_date, appointment_time, status, visit_type, parent_appointment_id, notes, created_by)
                VALUES (?, ?, ?, ?, '2025-03-01', '14:00:00', 'REGISTERED', 'FOLLOW_UP', ?,
                    'Upcoming follow-up', ?)
                """, UUID.randomUUID(), hospitalId, patientId, doctorId, appointmentId1, doctorId);
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/patients/{patientId}/history — success scenarios
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/patients/{patientId}/history — success")
    class GetPatientHistorySuccess {

        @Test
        @DisplayName("returns 200 OK with history entries for a valid patient")
        void getHistory_ValidPatient_Returns200WithHistory() throws Exception {
            mockMvc.perform(get(HISTORY_URL, patientId)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3));
        }

        @Test
        @DisplayName("history entries are ordered by date descending (most recent first)")
        void getHistory_MultipleAppointments_OrderedByDateDesc() throws Exception {
            mockMvc.perform(get(HISTORY_URL, patientId)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].date").value("2025-03-01"))
                    .andExpect(jsonPath("$[1].date").value("2024-12-20"))
                    .andExpect(jsonPath("$[2].date").value("2024-06-15"));
        }

        @Test
        @DisplayName("history entry contains expected fields")
        void getHistory_CompletedAppointment_ContainsAllFields() throws Exception {
            mockMvc.perform(get(HISTORY_URL, patientId)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    // Third entry (2024-06-15) — completed NEW_VISIT with consultation + prescription
                    .andExpect(jsonPath("$[2].appointmentId").exists())
                    .andExpect(jsonPath("$[2].date").value("2024-06-15"))
                    .andExpect(jsonPath("$[2].time").value("09:00:00"))
                    .andExpect(jsonPath("$[2].visitType").value("NEW_VISIT"))
                    .andExpect(jsonPath("$[2].status").value("COMPLETED"))
                    .andExpect(jsonPath("$[2].doctorName").value("Dr. History Test"))
                    .andExpect(jsonPath("$[2].diagnosis").value("Diabetic retinopathy screening - Normal"))
                    .andExpect(jsonPath("$[2].medicines").value("Artificial Tears"))
                    .andExpect(jsonPath("$[2].followUpDate").value("2025-01-20"))
                    .andExpect(jsonPath("$[2].notes").value("Routine eye checkup"));
        }

        @Test
        @DisplayName("history entry without prescription has null medicines")
        void getHistory_NoPrescription_MedicinesIsNull() throws Exception {
            mockMvc.perform(get(HISTORY_URL, patientId)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    // Second entry (2024-12-20) — completed FOLLOW_UP with consultation but no prescription
                    .andExpect(jsonPath("$[1].diagnosis").value("Cataracts - early stage"))
                    .andExpect(jsonPath("$[1].medicines").doesNotExist());
        }

        @Test
        @DisplayName("history entry without consultation has null diagnosis and medicines")
        void getHistory_NoConsultation_DiagnosisAndMedicinesNull() throws Exception {
            mockMvc.perform(get(HISTORY_URL, patientId)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    // First entry (2025-03-01) — registered, no consultation
                    .andExpect(jsonPath("$[0].status").value("REGISTERED"))
                    .andExpect(jsonPath("$[0].diagnosis").doesNotExist())
                    .andExpect(jsonPath("$[0].medicines").doesNotExist());
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/patients/{patientId}/history — error scenarios
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/patients/{patientId}/history — errors")
    class GetPatientHistoryErrors {

        @Test
        @DisplayName("returns 409 when patient does not exist")
        void getHistory_NonexistentPatient_Returns409() throws Exception {
            UUID fakeId = UUID.randomUUID();

            mockMvc.perform(get(HISTORY_URL, fakeId)
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 401 when no token is provided")
        void getHistory_NoToken_Returns401() throws Exception {
            mockMvc.perform(get(HISTORY_URL, patientId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/patients/{patientId}/history — tenant isolation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/patients/{patientId}/history — tenant isolation")
    class GetPatientHistoryTenantIsolation {

        @Test
        @DisplayName("returns 409 when patient belongs to a different hospital")
        void getHistory_DifferentHospital_Returns409() throws Exception {
            // Create a patient in a different hospital
            Hospital otherHospital = new Hospital();
            otherHospital.setName("Other Hospital");
            otherHospital.setSubdomain("other-" + UUID.randomUUID().toString().substring(0, 8));
            otherHospital.setAddress("456 Other Street");
            otherHospital.setContactEmail("other@hospital.com");
            otherHospital.setContactPhone("+91-0000000000");
            otherHospital.setActive(true);
            otherHospital = hospitalRepository.saveAndFlush(otherHospital);

            Patient otherPatient = new Patient();
            otherPatient.setHospital(otherHospital);
            otherPatient.setFullName("Other Patient");
            otherPatient.setMobileNumber("+91-" + UUID.randomUUID().toString().substring(0, 10));
            otherPatient.setAge(30);
            otherPatient.setGender("FEMALE");
            otherPatient = patientRepository.saveAndFlush(otherPatient);

            // Try to access the other hospital's patient with our token
            mockMvc.perform(get(HISTORY_URL, otherPatient.getPatientId())
                            .header("Authorization", "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }
    }
}
