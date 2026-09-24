package com.ddd.application.mapper;

import com.ddd.application.dto.auth.UserRegisterDto;
import com.ddd.application.dto.user.UserDto;
import com.ddd.domain.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {
    User toUser(UserDto userDto);

    UserDto toUserDto(User user);
}
