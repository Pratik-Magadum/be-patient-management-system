package com.eyehospital.pms.module.appointment.controllerImpl;

import java.util.UUID;

import org.springframework.web.bind.annotation.RestController;

import com.eyehospital.pms.common.exception.BusinessException;
import com.eyehospital.pms.module.appointment.controller.AppointmentController;
import com.eyehospital.pms.module.appointment.dto.FollowUpRequestDto;
import com.eyehospital.pms.module.appointment.dto.RegisterAppointmentRequestDto;
import com.eyehospital.pms.module.appointment.service.AppointmentService;
import com.eyehospital.pms.module.patient.dto.PatientSearchResponseDto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AppointmentControllerImpl implements AppointmentController {

    private final AppointmentService appointmentService;

    @Override
    public PatientSearchResponseDto registerAppointment(RegisterAppointmentRequestDto request,
                                                         HttpServletRequest httpRequest) {
        UUID hospitalId = extractHospitalId(httpRequest);
        log.info("POST /appointments/register - hospitalId={} patient={}",
                hospitalId, request.getFullName());
        return appointmentService.registerAppointment(hospitalId, request);
    }

    @Override
    public PatientSearchResponseDto registerFollowUp(FollowUpRequestDto request,
                                                      HttpServletRequest httpRequest) {
        UUID hospitalId = extractHospitalId(httpRequest);
        log.info("POST /appointments/follow-up - hospitalId={} parentId={}",
                hospitalId, request.getParentAppointmentId());
        return appointmentService.registerFollowUp(hospitalId, request);
    }

    @Override
    public void deleteAppointment(UUID appointmentId, HttpServletRequest httpRequest) {
        UUID hospitalId = extractHospitalId(httpRequest);
        log.info("DELETE /appointments/{} - hospitalId={}", appointmentId, hospitalId);
        appointmentService.deleteAppointment(hospitalId, appointmentId);
    }

    private UUID extractHospitalId(HttpServletRequest request) {
        String hospitalIdStr = (String) request.getAttribute("hospitalId");
        if (hospitalIdStr == null) {
            log.warn("hospitalId not found in request attributes");
            throw new BusinessException("MISSING_HOSPITAL_CONTEXT",
                    "Hospital context not found — invalid or missing JWT token");
        }
        return UUID.fromString(hospitalIdStr);
    }
}
