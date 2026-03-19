package com.eyehospital.pms.module.patient.controllerImpl;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.RestController;

import com.eyehospital.pms.common.exception.BusinessException;
import com.eyehospital.pms.module.patient.controller.PatientController;
import com.eyehospital.pms.module.patient.dto.PatientDashboardResponseDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchRequestDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchResponseDto;
import com.eyehospital.pms.module.patient.service.PatientService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller implementation for patient endpoints.
 *
 * <p>This class is intentionally thin — it only:</p>
 * <ol>
 *   <li>Extracts the {@code hospitalId} from the request attribute (set by TenantContextFilter).</li>
 *   <li>Delegates to {@link PatientService} for business logic.</li>
 *   <li>Returns the DTO directly — errors are handled by GlobalExceptionHandler.</li>
 * </ol>
 *
 * <p>All Swagger annotations are declared on the {@link PatientController}
 * interface. Constructor injection is used — no field injection.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PatientControllerImpl implements PatientController {

    private final PatientService patientService;

    @Override
    public PatientDashboardResponseDto getTodayDashboard(HttpServletRequest request) {

        String hospitalIdStr = (String) request.getAttribute("hospitalId");
        if (hospitalIdStr == null) {
            log.warn("GET /patients/dashboard/today - hospitalId not found in request attributes");
            throw new BusinessException("MISSING_HOSPITAL_CONTEXT", "Hospital context not found — invalid or missing JWT token");
        }
        UUID hospitalId = UUID.fromString(hospitalIdStr);

        log.info("GET /patients/dashboard/today - fetching dashboard for hospitalId={}", hospitalId);

        return patientService.getTodayDashboard(hospitalId);
    }

    @Override
    public List<PatientSearchResponseDto> getPatients(
            PatientSearchRequestDto searchRequest, HttpServletRequest request) {

        UUID hospitalId = extractHospitalId(request);

        // Validate: date range requires both fromDate and toDate
        if (searchRequest != null) {
            boolean hasFromDate = searchRequest.getFromDate() != null;
            boolean hasToDate = searchRequest.getToDate() != null;

            if (hasFromDate != hasToDate) {
                throw new BusinessException("INVALID_DATE_RANGE",
                        "Both fromDate and toDate are required for date range search");
            }

            // Validate: fromDate must be equal to or before toDate
            if (hasFromDate && searchRequest.getFromDate().isAfter(searchRequest.getToDate())) {
                throw new BusinessException("INVALID_DATE_RANGE",
                        "fromDate must be equal to or before toDate");
            }
        }

        log.info("GET /patients/search - hospitalId={}", hospitalId);

        return patientService.getPatients(hospitalId, searchRequest);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

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
