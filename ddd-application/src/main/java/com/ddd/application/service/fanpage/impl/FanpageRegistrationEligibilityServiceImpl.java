package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.fanpage.FanpageRegistrationEligibilityDto;
import com.ddd.application.service.fanpage.FanpageRegistrationEligibilityService;
import com.ddd.domain.exception.FanpageAdminProfileRequiredException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageAdminProfileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FanpageRegistrationEligibilityServiceImpl implements FanpageRegistrationEligibilityService {

    public static final String STATUS_ELIGIBLE = "ELIGIBLE";
    public static final String STATUS_PROFILE_REQUIRED = "PROFILE_REQUIRED";
    public static final String REASON_PROFILE_REQUIRED = "FANPAGE_ADMIN_PROFILE_REQUIRED";

    private final FanpageAdminProfileJpaRepository fanpageAdminProfileJpaRepository;
    private final SecurityContextPort securityContextPort;

    @Override
    public FanpageRegistrationEligibilityDto evaluateCurrentUser() {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        if (isProfileComplete(currentUserId)) {
            return new FanpageRegistrationEligibilityDto(STATUS_ELIGIBLE, null, false);
        }

        return new FanpageRegistrationEligibilityDto(STATUS_PROFILE_REQUIRED, REASON_PROFILE_REQUIRED, false);
    }

    @Override
    public void assertCurrentUserCanRegister() {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        if (!isProfileComplete(currentUserId)) {
            throw new FanpageAdminProfileRequiredException();
        }
    }

    private boolean isProfileComplete(Long userId) {
        return fanpageAdminProfileJpaRepository.findByUserId(userId)
                .map(p -> p.getStaffId() != null && !p.getStaffId().isBlank()
                        && p.getOrgName() != null && !p.getOrgName().isBlank())
                .orElse(false);
    }
}
