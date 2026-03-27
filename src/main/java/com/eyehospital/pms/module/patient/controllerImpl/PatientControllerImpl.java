package com.eyehospital.pms.module.patient.controllerImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.RestController;

import com.eyehospital.pms.common.exception.BusinessException;
import com.eyehospital.pms.module.patient.controller.PatientController;
import com.eyehospital.pms.module.patient.dto.PatientDashboardResponseDto;
import com.eyehospital.pms.module.patient.dto.PatientSearchListResponseDto;
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
    public PatientDashboardResponseDto getDashboard(LocalDate fromDate, LocalDate toDate,
                                                    HttpServletRequest request) {

        UUID hospitalId = extractHospitalId(request);

        // Validate: both dates must be provided together
        boolean hasFromDate = fromDate != null;
        boolean hasToDate = toDate != null;
        if (hasFromDate != hasToDate) {
            throw new BusinessException("INVALID_DATE_RANGE",
                    "Both fromDate and toDate are required for date range search");
        }

        // Validate: fromDate must be equal to or before toDate
        if (hasFromDate && fromDate.isAfter(toDate)) {
            throw new BusinessException("INVALID_DATE_RANGE",
                    "fromDate must be equal to or before toDate");
        }

        log.info("GET /patients/dashboard/today - fetching dashboard for hospitalId={} from={} to={}",
                hospitalId, fromDate, toDate);

        return patientService.getDashboard(hospitalId, fromDate, toDate);
    }

    @Override
    public PatientSearchListResponseDto getPatients(
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

            // Validate: page size must be positive and reasonable
            if (searchRequest.getSize() < 1 || searchRequest.getSize() > 100) {
                throw new BusinessException("INVALID_PAGE_SIZE",
                        "Page size must be between 1 and 100");
            }
            if (searchRequest.getPage() < 0) {
                throw new BusinessException("INVALID_PAGE_NUMBER",
                        "Page number must be 0 or greater");
            }
        }

        log.info("GET /patients/search - hospitalId={}", hospitalId);

        return patientService.getPatients(hospitalId, searchRequest);
    }

    @Override
    public List<PatientSearchResponseDto> searchByNamePhone(String name, String phoneNumber,
                                                            HttpServletRequest request) {
        UUID hospitalId = extractHospitalId(request);

        boolean hasName = name != null && !name.isBlank();
        boolean hasPhone = phoneNumber != null && !phoneNumber.isBlank();

        if (!hasName && !hasPhone) {
            throw new BusinessException("MISSING_SEARCH_CRITERIA",
                    "At least one of name or phoneNumber must be provided");
        }

        log.info("GET /patients/search/by-name-phone - hospitalId={} name={} phoneNumber={}",
                hospitalId, name, phoneNumber);

        return patientService.searchByNamePhone(hospitalId, name, phoneNumber);
    }

    @Override
    public void deletePatient(UUID patientId, HttpServletRequest request) {
        UUID hospitalId = extractHospitalId(request);
        log.info("DELETE /patients/{} - hospitalId={}", patientId, hospitalId);
        patientService.deletePatient(hospitalId, patientId);
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
