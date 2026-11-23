package com.ddd.application.service.userprofile.impl;

import com.ddd.application.dto.user.StaffProfileCreateDto;
import com.ddd.application.dto.user.StaffProfileDetailsCreateDto;
import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.application.mapper.StaffProfileDtoMapper;
import com.ddd.application.service.userprofile.StaffProfileCreationService;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.exception.StaffIdAlreadyExistsException;
import com.ddd.domain.exception.StaffProfileAlreadyExistsException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageAdminProfileJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffProfileCreationServiceImpl implements StaffProfileCreationService {

    private final SecurityContextPort securityContextPort;
    private final UserJpaRepository userJpaRepository;
    private final FanpageAdminProfileJpaRepository fanpageAdminProfileJpaRepository;
    private final StaffProfileDtoMapper staffProfileDtoMapper;

    @Override
    @Transactional
    public StaffProfileSummaryDto createForCurrentUser(StaffProfileCreateDto dto) {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required to create a staff profile");
        }

        UserJpaEntity user = userJpaRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        // Precheck 1: User already has a staff profile
        if (fanpageAdminProfileJpaRepository.findByUserId(currentUserId).isPresent()) {
            throw new StaffProfileAlreadyExistsException("User already has a staff profile.");
        }

        StaffProfileDetailsCreateDto detailsDto = dto.staffProfile();
        String normalizedStaffId = detailsDto.staffId().trim();

        // Precheck 2: Staff ID already exists
        if (fanpageAdminProfileJpaRepository.existsByStaffId(normalizedStaffId)) {
            throw new StaffIdAlreadyExistsException("Staff ID '" + normalizedStaffId + "' already exists.");
        }

        // Update allowed user fields
        user.setFirstName(dto.firstName().trim());
        user.setLastName(dto.lastName().trim());
        if (dto.gender() != null && !dto.gender().isBlank()) {
            user.setGender(dto.gender().trim());
        }
        userJpaRepository.save(user);

        // Create fanpage admin profile
        FanpageAdminProfileJpaEntity profile = new FanpageAdminProfileJpaEntity();
        profile.setUser(user);
        profile.setStaffId(normalizedStaffId);
        profile.setOrgName(detailsDto.orgName().trim());
        profile.setTitle(detailsDto.title().trim());
        profile.setDob(detailsDto.dateOfBirth());
        if (detailsDto.phoneNumber() != null && !detailsDto.phoneNumber().isBlank()) {
            profile.setPhoneNumber(detailsDto.phoneNumber().trim());
        } else {
            profile.setPhoneNumber(null);
        }

        FanpageAdminProfileJpaEntity savedProfile = fanpageAdminProfileJpaRepository.save(profile);
        log.info("Staff profile created successfully for user id: {}, staffId: {}", currentUserId, normalizedStaffId);

        return staffProfileDtoMapper.toStaffProfileSummaryDto(user, savedProfile);
    }
}
