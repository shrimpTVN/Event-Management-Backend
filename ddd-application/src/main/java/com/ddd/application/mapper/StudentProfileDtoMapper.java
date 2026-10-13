package com.ddd.application.mapper;

import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.dto.user.StudentProfileSummaryDto;
import com.ddd.domain.model.StudentProfile;
import com.ddd.infrastructure.entity.RoleJpaEntity;
import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface StudentProfileDtoMapper {
    @Mapping(source = "KNumber", target = "kNumber")
    StudentProfileDto toStudentProfileDto(@Valid StudentProfile  studentProfile);

    @Mapping(source = "kNumber", target = "KNumber")
    StudentProfile toStudentProfile(StudentProfileDto studentProfileDto);

    @Mapping(source = "user.email", target = "email")
    @Mapping(target = "roles", expression = "java(mapRoleNames(studentProfileJpaEntity.getUser() != null ? studentProfileJpaEntity.getUser().getRoles() : null))")
    @Mapping(target = "role", expression = "java(mapPrimaryRole(studentProfileJpaEntity.getUser() != null ? studentProfileJpaEntity.getUser().getRoles() : null))")
    @Mapping(source = "user.isActive", target = "isActive")
    @Mapping(source = "KNumber", target = "kNumber")
    @Mapping(source = "association.name", target = "associationName")
    @Mapping(source = "major.name", target = "majorName")
    @Mapping(source = "major.code", target = "majorCode")
    @Mapping(source = "major.school.name", target = "schoolName")
    StudentProfileSummaryDto toStudentProfileSummaryDto(StudentProfileJpaEntity studentProfileJpaEntity);

    default Set<String> mapRoleNames(Set<RoleJpaEntity> roles) {
        if (roles == null) {
            return Collections.emptySet();
        }
        return roles.stream()
                .map(RoleJpaEntity::getName)
                .collect(Collectors.toSet());
    }

    default String mapPrimaryRole(Set<RoleJpaEntity> roles) {
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        return roles.iterator().next().getName();
    }
}
