package com.ddd.application.mapper;

import com.ddd.application.dto.auth.UserRegisterDto;
import com.ddd.domain.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {
    User toUser(UserRegisterDto userRegisterDto);
}
