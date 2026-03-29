package com.eyehospital.pms.module.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to register a new patient appointment")
public class RegisterAppointmentRequestDto {

    @NotBlank(message = "fullName is required")
    @Schema(description = "Patient full name", example = "Rajesh Kumar")
    private String fullName;

    @NotBlank(message = "mobileNumber is required")
    @Schema(description = "Patient mobile number", example = "+91-9800000001")
    private String mobileNumber;

    @Schema(description = "Patient email address", example = "rajesh@email.com")
    private String email;

    @NotNull(message = "age is required")
    @Schema(description = "Patient age", example = "35")
    private Integer age;

    @Schema(description = "Patient gender: MALE, FEMALE, or OTHER", example = "MALE")
    private String gender;

    @Schema(description = "Patient date of birth (ISO format)", example = "1990-05-15")
    private LocalDate dateOfBirth;

    @Schema(description = "Patient address", example = "12 MG Road, Mumbai")
    private String address;

    @NotNull(message = "appointmentDate is required")
    @Schema(description = "Date for the appointment (ISO format)", example = "2026-04-01")
    private LocalDate appointmentDate;

    @NotNull(message = "appointmentTime is required")
    @Schema(description = "Time for the appointment", example = "10:30:00")
    private LocalTime appointmentTime;

    @Schema(description = "Optional notes for the appointment", example = "Eye checkup")
    private String notes;
}
