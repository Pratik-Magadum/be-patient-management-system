package com.eyehospital.pms.infrastructure.tenant.serviceImpl;

import com.eyehospital.pms.common.exception.ResourceNotFoundException;
import com.eyehospital.pms.infrastructure.tenant.dto.HospitalResponseDto;
import com.eyehospital.pms.infrastructure.tenant.entity.Hospital;
import com.eyehospital.pms.infrastructure.tenant.repository.HospitalRepository;
import com.eyehospital.pms.infrastructure.tenant.service.HospitalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link HospitalService}.
 *
 * <p>Responsibilities (Single Responsibility principle):</p>
 * <ul>
 *   <li>Delegate persistence operations to {@link HospitalRepository}.</li>
 *   <li>Map the {@link Hospital} entity to a {@link HospitalResponseDto}.</li>
 *   <li>Throw domain exceptions when business invariants are violated.</li>
 * </ul>
 *
 * <p>Constructor injection is used exclusively — no field injection.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalServiceImpl implements HospitalService {

    private final HospitalRepository hospitalRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Only <em>active</em> hospitals are returned. If the subdomain maps to
     * an inactive or non-existent hospital a {@link ResourceNotFoundException}
     * is thrown, which the {@code GlobalExceptionHandler} translates to
     * {@code 404 NOT FOUND}.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public HospitalResponseDto getHospitalBySubdomain(String subdomain) {
        log.debug("Looking up hospital by subdomain: {}", subdomain);

        Hospital hospital = hospitalRepository
                .findBySubdomainAndActiveTrue(subdomain)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", "subdomain", subdomain));

        log.debug("Found hospital '{}' for subdomain '{}'", hospital.getName(), subdomain);
        return toResponseDto(hospital);
    }

    // -----------------------------------------------------------------------
    // Private mapping helper (keeps mapping logic in one place — DRY)
    // -----------------------------------------------------------------------

    private HospitalResponseDto toResponseDto(Hospital hospital) {
        return HospitalResponseDto.builder()
                .hospitalId(hospital.getHospitalId())
                .name(hospital.getName())
                .subdomain(hospital.getSubdomain())
                .address(hospital.getAddress())
                .contactEmail(hospital.getContactEmail())
                .contactPhone(hospital.getContactPhone())
                .active(hospital.isActive())
                .createdAt(hospital.getCreatedAt())
                .updatedAt(hospital.getUpdatedAt())
                .build();
    }
}
