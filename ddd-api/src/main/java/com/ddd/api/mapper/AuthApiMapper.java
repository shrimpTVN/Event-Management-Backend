package com.ddd.api.mapper;

import com.ddd.api.dto.auth.req.RegisterRequestDto;
import com.ddd.api.dto.auth.res.LoginResponseDto;
import com.ddd.application.dto.auth.LoginDto;
import com.ddd.application.dto.auth.UserRegisterDto;
import com.ddd.application.dto.user.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthApiMapper {

    LoginResponseDto toLoginResponse(LoginDto loginDto);

    UserDto toUserDto(RegisterRequestDto registerRequestDto);

    com.ddd.application.dto.user.StudentProfileDto toUserProfileDto(RegisterRequestDto userRegisterRequestDto);
}
