package com.eyehospital.pms.infrastructure.tenant.service;

import com.eyehospital.pms.infrastructure.tenant.dto.HospitalResponseDto;

/**
 * Contract for hospital lookup operations.
 *
 * <p>Declaring behaviour as an interface (Open/Closed principle) ensures the
 * service layer can be extended or swapped without altering callers.</p>
 */
public interface HospitalService {

    /**
     * Retrieves hospital information by its unique subdomain.
     *
     * @param subdomain the tenant subdomain slug (e.g. {@code apollo-eye})
     * @return the matching hospital's data as a response DTO
     * @throws com.eyehospital.pms.common.exception.ResourceNotFoundException
     *         if no active hospital is found for the given subdomain
     */
    HospitalResponseDto getHospitalBySubdomain(String subdomain);
}
