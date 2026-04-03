package com.eyehospital.pms.module.appointment.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.eyehospital.pms.common.enums.AppointmentStatus;
import com.eyehospital.pms.module.appointment.entity.Appointment;
import com.eyehospital.pms.module.patient.entity.Patient;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;

public final class AppointmentSpecification {

    private AppointmentSpecification() {
    }

    public static Specification<Appointment> hasHospitalId(UUID hospitalId) {
        return (root, query, cb) -> cb.equal(root.get("hospitalId"), hospitalId);
    }

    public static Specification<Appointment> patientNotDeleted() {
        return (root, query, cb) -> {
            Join<Appointment, Patient> patient = root.join("patient");
            return cb.equal(patient.get("deleted"), false);
        };
    }

    public static Specification<Appointment> appointmentNotDeleted() {
        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<Appointment> patientNameContains(String name) {
        return (root, query, cb) -> {
            Join<Appointment, Patient> patient = root.join("patient");
            return cb.like(cb.lower(patient.get("fullName")),
                    "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<Appointment> patientPhoneContains(String phone) {
        return (root, query, cb) -> {
            Join<Appointment, Patient> patient = root.join("patient");
            return cb.like(patient.get("mobileNumber"), "%" + phone + "%");
        };
    }

    public static Specification<Appointment> hasStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Appointment> hasVisitType(String visitType) {
        return (root, query, cb) -> cb.equal(root.get("visitType"), visitType);
    }

    public static Specification<Appointment> onDate(LocalDate date) {
        return (root, query, cb) -> cb.equal(root.get("appointmentDate"), date);
    }

    public static Specification<Appointment> betweenDates(LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) -> cb.between(root.get("appointmentDate"), fromDate, toDate);
    }

    public static Specification<Appointment> orderByDateAndTimeDesc() {
        return (root, query, cb) -> {
            query.orderBy(
                    cb.desc(root.get("appointmentDate")),
                    cb.desc(root.get("appointmentTime")));
            return cb.conjunction();
        };
    }

    public static Specification<Appointment> orderByStatusThenDateTimeAsc() {
        return (root, query, cb) -> {
            // REGISTERED=0, IN_PROGRESS=1, COMPLETED=2
            Expression<Integer> statusOrder = cb.<Integer>selectCase()
                    .when(cb.equal(root.get("status"), AppointmentStatus.REGISTERED.name()), 0)
                    .when(cb.equal(root.get("status"), AppointmentStatus.IN_PROGRESS.name()), 1)
                    .when(cb.equal(root.get("status"), AppointmentStatus.COMPLETED.name()), 2)
                    .otherwise(3);

            query.orderBy(List.of(
                    cb.asc(statusOrder),
                    cb.asc(root.get("appointmentDate")),
                    cb.asc(root.get("appointmentTime"))));
            return cb.conjunction();
        };
    }
}
