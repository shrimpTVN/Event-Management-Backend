package com.ddd.api.mapper;

import com.ddd.application.dto.auth.LoginDto;
import com.ddd.application.dto.auth.UserRegisterDto;
import com.ddd.api.dto.auth.req.UserRegisterRequestDto;
import com.ddd.api.dto.auth.res.LoginResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthApiMapper {

    LoginResponseDto toLoginResponse(LoginDto loginDto);
    UserRegisterDto toUserRegisterDto(UserRegisterRequestDto userRegisterRequestDto);
}
