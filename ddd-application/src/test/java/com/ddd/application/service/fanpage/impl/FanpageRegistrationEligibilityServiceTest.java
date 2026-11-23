package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.fanpage.FanpageRegistrationEligibilityDto;
import com.ddd.domain.exception.FanpageAdminProfileRequiredException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageAdminProfileJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FanpageRegistrationEligibilityServiceTest {

    @Mock
    private FanpageAdminProfileJpaRepository fanpageAdminProfileJpaRepository;

    @Mock
    private SecurityContextPort securityContextPort;

    @InjectMocks
    private FanpageRegistrationEligibilityServiceImpl eligibilityService;

    private final Long userId = 10L;

    @Test
    @DisplayName("evaluateCurrentUser should return ELIGIBLE when profile is complete")
    void evaluateCurrentUser_whenProfileComplete_shouldReturnEligible() {
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);

        FanpageAdminProfileJpaEntity profile = new FanpageAdminProfileJpaEntity();
        profile.setStaffId("CB0123");
        profile.setOrgName("School of CS");
        when(fanpageAdminProfileJpaRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        FanpageRegistrationEligibilityDto result = eligibilityService.evaluateCurrentUser();

        assertNotNull(result);
        assertEquals("ELIGIBLE", result.status());
        assertNull(result.reasonCode());
        assertFalse(result.requiresReauthentication());
    }

    @Test
    @DisplayName("evaluateCurrentUser should return PROFILE_REQUIRED when profile is missing")
    void evaluateCurrentUser_whenProfileMissing_shouldReturnProfileRequired() {
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(fanpageAdminProfileJpaRepository.findByUserId(userId)).thenReturn(Optional.empty());

        FanpageRegistrationEligibilityDto result = eligibilityService.evaluateCurrentUser();

        assertNotNull(result);
        assertEquals("PROFILE_REQUIRED", result.status());
        assertEquals("FANPAGE_ADMIN_PROFILE_REQUIRED", result.reasonCode());
        assertFalse(result.requiresReauthentication());
    }

    @Test
    @DisplayName("evaluateCurrentUser should return PROFILE_REQUIRED when staffId is blank")
    void evaluateCurrentUser_whenStaffIdBlank_shouldReturnProfileRequired() {
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);

        FanpageAdminProfileJpaEntity profile = new FanpageAdminProfileJpaEntity();
        profile.setStaffId("   ");
        profile.setOrgName("School of CS");
        when(fanpageAdminProfileJpaRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        FanpageRegistrationEligibilityDto result = eligibilityService.evaluateCurrentUser();

        assertNotNull(result);
        assertEquals("PROFILE_REQUIRED", result.status());
        assertEquals("FANPAGE_ADMIN_PROFILE_REQUIRED", result.reasonCode());
    }

    @Test
    @DisplayName("evaluateCurrentUser should throw UnauthorizedException when user unauthenticated")
    void evaluateCurrentUser_whenUnauthenticated_shouldThrow() {
        when(securityContextPort.getCurrentUserId()).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> eligibilityService.evaluateCurrentUser());
    }

    @Test
    @DisplayName("assertCurrentUserCanRegister should succeed when profile complete")
    void assertCurrentUserCanRegister_whenComplete_shouldNotThrow() {
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);

        FanpageAdminProfileJpaEntity profile = new FanpageAdminProfileJpaEntity();
        profile.setStaffId("CB0123");
        profile.setOrgName("School of CS");
        when(fanpageAdminProfileJpaRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        assertDoesNotThrow(() -> eligibilityService.assertCurrentUserCanRegister());
    }

    @Test
    @DisplayName("assertCurrentUserCanRegister should throw FanpageAdminProfileRequiredException when incomplete")
    void assertCurrentUserCanRegister_whenIncomplete_shouldThrow() {
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(fanpageAdminProfileJpaRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(FanpageAdminProfileRequiredException.class, () -> eligibilityService.assertCurrentUserCanRegister());
    }
}
