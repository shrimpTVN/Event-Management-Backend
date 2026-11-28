package com.ddd.application.service.user.impl;

import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.application.dto.user.StudentProfileSummaryDto;
import com.ddd.application.dto.user.UserInfoDto;
import com.ddd.application.dto.user.UserSummaryDto;
import com.ddd.application.mapper.StaffProfileDtoMapper;
import com.ddd.application.mapper.StudentProfileDtoMapper;
import com.ddd.application.mapper.UserDtoMapper;
import com.ddd.application.service.user.UserQueryService;
import com.ddd.application.utils.ServiceUtil;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageAdminProfileJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.StudentProfileJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import com.ddd.infrastructure.repository.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {
    private static final int INVITATION_SEARCH_LIMIT = 10;

    private final StudentProfileJpaRepository studentProfileJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final FanpageAdminProfileJpaRepository fanpageAdminProfileJpaRepository;
    private final StudentProfileDtoMapper studentProfileDtoMapper;
    private final StaffProfileDtoMapper staffProfileDtoMapper;
    private final UserDtoMapper userDtoMapper;
    private final SecurityContextPort securityContextPort;

    @Override
    public List<UserSummaryDto> searchUsers(String keyword) {
        Pageable pageable = PageRequest.of(0, INVITATION_SEARCH_LIMIT, Sort.by(Sort.Direction.ASC, "email"));
        Specification<UserJpaEntity> spec = UserSpecification.searchForInvitation(keyword);

        return userJpaRepository.findAll(spec, pageable)
                .map(userDtoMapper::toUserSummaryDto)
                .getContent();
    }

    @Override
    public UserInfoDto getCurrentUserProfileByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("User is not authenticated");
        }
        UserJpaEntity user = userJpaRepository.findDetailedByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return UserInfoAssembler.toUserInfoDto(user, user.getRoles(), staffProfileDtoMapper);
    }

    @Override
    public StudentProfileSummaryDto getProfileStudentByEmail(String email) {
        StudentProfileJpaEntity studentProfileJpaEntity = studentProfileJpaRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for email: " + email));
        return studentProfileDtoMapper.toStudentProfileSummaryDto(studentProfileJpaEntity);
    }

    @Override
    public StudentProfileSummaryDto getProfileStudentById(Long id) {
        StudentProfileJpaEntity studentProfileJpaEntity = studentProfileJpaRepository.findByUserId(id)
                .or(() -> studentProfileJpaRepository.findByIdWithDetails(id))
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for id: " + id));
        return studentProfileDtoMapper.toStudentProfileSummaryDto(studentProfileJpaEntity);
    }

    @Override
    public Page<StudentProfileSummaryDto> getAllProfiles(boolean isActive, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = ServiceUtil.createPageable(page, size, sortBy, sortDir);

        return studentProfileJpaRepository.findAllByIsActive(isActive, pageable)
                .map(studentProfileDtoMapper::toStudentProfileSummaryDto);
    }

    @Override
    public Page<UserSummaryDto> getAllUsers(boolean isActive, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = ServiceUtil.createPageable(page, size, sortBy, sortDir);
        return userJpaRepository.findAllByIsActive(isActive, pageable)
                .map(userDtoMapper::toUserSummaryDto);
    }

    @Override
    public StaffProfileSummaryDto getStaffProfile() {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        UserJpaEntity user = userJpaRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        FanpageAdminProfileJpaEntity staffProfile = fanpageAdminProfileJpaRepository.findByUserId(currentUserId)
                .orElse(null);

        return staffProfileDtoMapper.toStaffProfileSummaryDto(user, staffProfile);
    }
}
