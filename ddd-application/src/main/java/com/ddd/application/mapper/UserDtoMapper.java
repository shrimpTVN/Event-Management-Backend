package com.ddd.application.mapper;

import com.ddd.application.dto.auth.UserRegisterDto;
import com.ddd.application.dto.user.UserDto;
import com.ddd.application.dto.user.UserSummaryDto;
import com.ddd.domain.model.User;
import com.ddd.infrastructure.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {
    User toUser(UserDto userDto);

    UserDto toUserDto(User user);

    @Mapping(target="role", source="userJpaEntity.role.name")
    UserSummaryDto toUserSummaryDto(UserJpaEntity userJpaEntity);
}
