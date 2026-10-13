package com.ddd.application.mapper;

import com.ddd.application.dto.auth.UserRegisterDto;
import com.ddd.application.dto.user.UserDto;
import com.ddd.application.dto.user.UserSummaryDto;
import com.ddd.domain.model.User;
import com.ddd.infrastructure.entity.RoleJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {
    User toUser(UserDto userDto);

    UserDto toUserDto(User user);

    @Mapping(target = "roles", expression = "java(mapRoleNames(userJpaEntity.getRoles()))")
    @Mapping(target = "role", expression = "java(mapPrimaryRole(userJpaEntity.getRoles()))")
    UserSummaryDto toUserSummaryDto(UserJpaEntity userJpaEntity);

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
            return null;
        }
        return roles.iterator().next().getName();
    }
}
