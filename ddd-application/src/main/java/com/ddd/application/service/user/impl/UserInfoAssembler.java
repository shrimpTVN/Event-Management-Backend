package com.ddd.application.service.user.impl;

import com.ddd.application.dto.user.StaffProfileDetailsDto;
import com.ddd.application.dto.user.StudentProfileDetailsDto;
import com.ddd.application.dto.user.UserInfoDto;
import com.ddd.application.mapper.StaffProfileDtoMapper;
import com.ddd.infrastructure.entity.RoleJpaEntity;
import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared mapping helper from {@link UserJpaEntity} to {@link UserInfoDto}
 * including studentProfile and fanpageAdminProfile (staffProfile).
 */
final class UserInfoAssembler {

    private UserInfoAssembler() {
    }

    static UserInfoDto toUserInfoDto(
            UserJpaEntity user,
            Set<RoleJpaEntity> roles,
            StaffProfileDtoMapper staffProfileDtoMapper
    ) {
        Set<String> roleNames = roles != null
                ? roles.stream().map(RoleJpaEntity::getName).collect(Collectors.toSet())
                : Collections.emptySet();

        String primaryRole = roleNames.isEmpty() ? null : roleNames.iterator().next();

        StudentProfileDetailsDto studentDetails = null;
        if (user.getStudentProfile() != null) {
            StudentProfileJpaEntity sp = user.getStudentProfile();
            Long majorId = sp.getMajor() != null ? sp.getMajor().getId() : null;
            String majorName = sp.getMajor() != null ? sp.getMajor().getName() : null;
            String majorCode = sp.getMajor() != null ? sp.getMajor().getCode() : null;
            Long schoolId = (sp.getMajor() != null && sp.getMajor().getSchool() != null)
                    ? sp.getMajor().getSchool().getId() : null;
            String schoolName = (sp.getMajor() != null && sp.getMajor().getSchool() != null)
                    ? sp.getMajor().getSchool().getName() : null;

            studentDetails = new StudentProfileDetailsDto(
                    sp.getStudentId(),
                    sp.getKNumber(),
                    schoolName,
                    majorName,
                    majorCode,
                    null,
                    schoolId,
                    majorId
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
