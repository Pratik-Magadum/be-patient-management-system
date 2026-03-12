package com.eyehospital.pms.module.patient.controllerImpl;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.eyehospital.pms.common.dto.ApiResponseDto;
import com.eyehospital.pms.module.patient.controller.PatientController;
import com.eyehospital.pms.module.patient.dto.PatientDashboardResponseDto;
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
 *   <li>Wraps the result in the standard {@link ApiResponseDto} envelope.</li>
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
    public ResponseEntity<ApiResponseDto<PatientDashboardResponseDto>> getTodayDashboard(
            HttpServletRequest request) {

        String hospitalIdStr = (String) request.getAttribute("hospitalId");
        if (hospitalIdStr == null) {
            log.warn("GET /patients/dashboard/today - hospitalId not found in request attributes");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseDto.success(
                            HttpStatus.UNAUTHORIZED.value(),
                            "Hospital context not found — invalid or missing JWT token",
                            null));
        }
        UUID hospitalId = UUID.fromString(hospitalIdStr);

        log.info("GET /patients/dashboard/today - fetching dashboard for hospitalId={}", hospitalId);

        PatientDashboardResponseDto dashboard = patientService.getTodayDashboard(hospitalId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponseDto.success(
                        HttpStatus.OK.value(),
                        "Patient dashboard retrieved successfully",
                        dashboard));
    }
}
