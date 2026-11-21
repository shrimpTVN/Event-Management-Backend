package com.ddd.application.mapper;

import com.ddd.application.dto.user.StaffProfileDetailsDto;
import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import com.ddd.infrastructure.entity.RoleJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface StaffProfileDtoMapper {

    @Mapping(target = "dateOfBirth", source = "dob")
    StaffProfileDetailsDto toStaffProfileDetailsDto(FanpageAdminProfileJpaEntity entity);

    default StaffProfileSummaryDto toStaffProfileSummaryDto(UserJpaEntity userEntity, FanpageAdminProfileJpaEntity staffProfileEntity) {
        if (userEntity == null) {
            return null;
        }
        Set<String> roles = mapRoleNames(userEntity.getRoles());
        StaffProfileDetailsDto staffDetails = staffProfileEntity != null ? toStaffProfileDetailsDto(staffProfileEntity) : null;
        return new StaffProfileSummaryDto(
                userEntity.getId(),
                userEntity.getFirstName(),
                userEntity.getLastName(),
                userEntity.getEmail(),
                userEntity.getGender(),
                userEntity.getAvatarUrl(),
                roles,
                staffDetails
        );
    }

    default Set<String> mapRoleNames(Set<RoleJpaEntity> roles) {
        if (roles == null) {
            return Collections.emptySet();
        }
        return roles.stream()
                .map(RoleJpaEntity::getName)
                .collect(Collectors.toSet());
    }
}
