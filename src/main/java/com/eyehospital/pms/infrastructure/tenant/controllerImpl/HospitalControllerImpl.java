package com.eyehospital.pms.infrastructure.tenant.controllerImpl;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.eyehospital.pms.common.response.ApiResponse;
import com.eyehospital.pms.infrastructure.tenant.controller.HospitalController;
import com.eyehospital.pms.infrastructure.tenant.dto.HospitalResponseDto;
import com.eyehospital.pms.infrastructure.tenant.service.HospitalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller implementation for hospital / tenant endpoints.
 *
 * <p>This class is intentionally thin — it only:</p>
 * <ol>
 *   <li>Delegates to {@link HospitalService} for business logic.</li>
 *   <li>Wraps the result in the standard {@link ApiResponse} envelope.</li>
 *   <li>Returns the appropriate HTTP status code.</li>
 * </ol>
 *
 * <p>All Swagger annotations are declared on the {@link HospitalController}
 * interface. Constructor injection is used — no field injection.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HospitalControllerImpl implements HospitalController {

    private final HospitalService hospitalService;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<ApiResponse<HospitalResponseDto>> getHospitalBySubdomain(
            @PathVariable String subdomain) {

        log.info("GET /hospitals/{} - fetching hospital by subdomain", subdomain);

        HospitalResponseDto response = hospitalService.getHospitalBySubdomain(subdomain);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Hospital retrieved successfully",
                        response));
    }
}
