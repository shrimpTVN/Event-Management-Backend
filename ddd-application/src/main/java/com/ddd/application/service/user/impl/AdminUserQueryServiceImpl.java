package com.ddd.application.service.user.impl;

import com.ddd.application.dto.user.StaffProfileDetailsDto;
import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.application.dto.user.StudentProfileDetailsDto;
import com.ddd.application.dto.user.UserInfoDto;
import com.ddd.application.mapper.StaffProfileDtoMapper;
import com.ddd.application.service.user.AdminUserQueryService;
import com.ddd.application.utils.ServiceUtil;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.infrastructure.entity.RoleJpaEntity;
import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminUserQueryServiceImpl implements AdminUserQueryService {

    private final UserJpaRepository userJpaRepository;
    private final StaffProfileDtoMapper staffProfileDtoMapper;

    @Override
    public Page<UserInfoDto> getAllUsers(boolean isActive, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = ServiceUtil.createPageable(page, size, sortBy, sortDir);
        Sort sort = pageable.getSort();
        if (sort.getOrderFor("id") == null) {
            Sort.Direction direction = sort.isSorted() ? sort.iterator().next().getDirection() : Sort.Direction.DESC;
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.and(Sort.by(direction, "id")));
        }

        Page<UserJpaEntity> userPage = userJpaRepository.findAllAdminUsersByIsActive(isActive, pageable);
        if (userPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> userIds = userPage.getContent().stream()
                .map(UserJpaEntity::getId)
                .toList();

        Map<Long, Set<RoleJpaEntity>> rolesByUserId = userJpaRepository.findUsersWithRolesByIdIn(userIds).stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, UserJpaEntity::getRoles, (a, b) -> a));

        return userPage.map(user -> toUserInfoDto(user, rolesByUserId.get(user.getId())));
    }

    @Override
    public StaffProfileSummaryDto getStaffProfileByUserId(Long userId) {
        UserJpaEntity user = userJpaRepository.findAdminProfileById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return staffProfileDtoMapper.toStaffProfileSummaryDto(user, user.getFanpageAdminProfile());
    }

    private UserInfoDto toUserInfoDto(UserJpaEntity user, Set<RoleJpaEntity> roles) {
        Set<String> roleNames = roles != null
                ? roles.stream().map(RoleJpaEntity::getName).collect(Collectors.toSet())
                : Collections.emptySet();

        String primaryRole = roleNames.isEmpty() ? null : roleNames.iterator().next();

        StudentProfileDetailsDto studentDetails = null;
        if (user.getStudentProfile() != null) {
            StudentProfileJpaEntity sp = user.getStudentProfile();
            String majorName = sp.getMajor() != null ? sp.getMajor().getName() : null;
            String majorCode = sp.getMajor() != null ? sp.getMajor().getCode() : null;
            String schoolName = (sp.getMajor() != null && sp.getMajor().getSchool() != null)
                    ? sp.getMajor().getSchool().getName() : null;

            studentDetails = new StudentProfileDetailsDto(
                    sp.getStudentId(),
                    sp.getKNumber(),
                    schoolName,
                    majorName,
                    majorCode,
                    null
            );
        }

        StaffProfileDetailsDto staffDetails = null;
        if (user.getFanpageAdminProfile() != null) {
            staffDetails = staffProfileDtoMapper.toStaffProfileDetailsDto(user.getFanpageAdminProfile());
        }

        return new UserInfoDto(
                user.getId(),
                user.getEmail(),
                roleNames,
                primaryRole,
                user.getFirstName(),
                user.getLastName(),
                user.getGender(),
                user.getIsActive(),
                user.getProviderId(),
                user.getAvatarUrl(),
                studentDetails,
                staffDetails
        );
    }
}
