package com.eyehospital.pms.infrastructure.tenant;

import com.eyehospital.pms.common.exception.ResourceNotFoundException;
import com.eyehospital.pms.infrastructure.tenant.dto.HospitalResponseDto;
import com.eyehospital.pms.infrastructure.tenant.entity.Hospital;
import com.eyehospital.pms.infrastructure.tenant.repository.HospitalRepository;
import com.eyehospital.pms.infrastructure.tenant.serviceImpl.HospitalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link HospitalServiceImpl}.
 *
 * <p>The repository is mocked so these tests run without any infrastructure
 * (no DB, no Spring context) — fast feedback cycle aligned with TDD.</p>
 *
 * <p>Naming convention: {@code methodName_StateUnderTest_ExpectedBehavior}</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HospitalServiceImpl — Unit Tests")
class HospitalServiceImplTest {

    @Mock
    private HospitalRepository hospitalRepository;

    @InjectMocks
    private HospitalServiceImpl hospitalService;

    private Hospital activeHospital;

    @BeforeEach
    void setUp() {
        activeHospital = new Hospital();
        activeHospital.setHospitalId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        activeHospital.setName("Apollo Eye Hospital");
        activeHospital.setSubdomain("apollo-eye");
        activeHospital.setAddress("12, Park Street, Kolkata - 700016");
        activeHospital.setContactEmail("contact@apollo-eye.com");
        activeHospital.setContactPhone("+91 9800000001");
        activeHospital.setActive(true);
    }

    // -----------------------------------------------------------------------
    // getHospitalBySubdomain
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getHospitalBySubdomain")
    class GetHospitalBySubdomain {

        @Test
        @DisplayName("returns DTO when active hospital exists for subdomain")
        void getHospitalBySubdomain_ActiveHospitalExists_ReturnsDto() {
            // ARRANGE
            when(hospitalRepository.findBySubdomainAndActiveTrue("apollo-eye"))
                    .thenReturn(Optional.of(activeHospital));

            // ACT
            HospitalResponseDto result = hospitalService.getHospitalBySubdomain("apollo-eye");

            // ASSERT
            assertThat(result).isNotNull();
            assertThat(result.getSubdomain()).isEqualTo("apollo-eye");
            assertThat(result.getName()).isEqualTo("Apollo Eye Hospital");
            assertThat(result.getHospitalId()).isEqualTo(activeHospital.getHospitalId());
            assertThat(result.getContactEmail()).isEqualTo("contact@apollo-eye.com");
            assertThat(result.isActive()).isTrue();

            verify(hospitalRepository, times(1)).findBySubdomainAndActiveTrue("apollo-eye");
        }

        @Test
        @DisplayName("maps all fields correctly from entity to DTO")
        void getHospitalBySubdomain_ActiveHospitalExists_MapsAllFieldsCorrectly() {
            // ARRANGE
            LocalDateTime now = LocalDateTime.now();
            activeHospital.setAddress("12, Park Street, Kolkata");
            activeHospital.setContactPhone("+91 9800000001");

            when(hospitalRepository.findBySubdomainAndActiveTrue("apollo-eye"))
                    .thenReturn(Optional.of(activeHospital));

            // ACT
            HospitalResponseDto result = hospitalService.getHospitalBySubdomain("apollo-eye");

            // ASSERT
            assertThat(result.getAddress()).isEqualTo("12, Park Street, Kolkata");
            assertThat(result.getContactPhone()).isEqualTo("+91 9800000001");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when no active hospital found")
        void getHospitalBySubdomain_NoActiveHospital_ThrowsResourceNotFoundException() {
            // ARRANGE
            when(hospitalRepository.findBySubdomainAndActiveTrue("unknown-hospital"))
                    .thenReturn(Optional.empty());

            // ACT & ASSERT
            assertThatThrownBy(() -> hospitalService.getHospitalBySubdomain("unknown-hospital"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Hospital")
                    .hasMessageContaining("subdomain")
                    .hasMessageContaining("unknown-hospital");

            verify(hospitalRepository, times(1)).findBySubdomainAndActiveTrue("unknown-hospital");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for inactive hospital subdomain")
        void getHospitalBySubdomain_InactiveHospital_ThrowsResourceNotFoundException() {
            // ARRANGE — inactive hospital: repository returns empty because query filters active=true
            when(hospitalRepository.findBySubdomainAndActiveTrue("inactive-hospital"))
                    .thenReturn(Optional.empty());

            // ACT & ASSERT
            assertThatThrownBy(() -> hospitalService.getHospitalBySubdomain("inactive-hospital"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("calls repository exactly once per invocation")
        void getHospitalBySubdomain_Called_InvokesRepositoryOnce() {
            // ARRANGE
            when(hospitalRepository.findBySubdomainAndActiveTrue("apollo-eye"))
                    .thenReturn(Optional.of(activeHospital));

            // ACT
            hospitalService.getHospitalBySubdomain("apollo-eye");

            // ASSERT
            verify(hospitalRepository, times(1)).findBySubdomainAndActiveTrue("apollo-eye");
            verifyNoMoreInteractions(hospitalRepository);
        }
    }
}
