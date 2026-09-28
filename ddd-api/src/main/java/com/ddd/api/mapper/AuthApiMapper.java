package com.ddd.api.mapper;

import com.ddd.api.dto.auth.req.FanpageAdminRegisterRequestDto;
import com.ddd.api.dto.auth.req.RegisterRequestDto;
import com.ddd.api.dto.auth.res.LoginResponseDto;
import com.ddd.application.dto.auth.LoginDto;
import com.ddd.application.dto.user.FanpageAdminProfileDto;
import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.dto.user.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthApiMapper {

    LoginResponseDto toLoginResponse(LoginDto loginDto);

    UserDto toUserDto(RegisterRequestDto registerRequestDto);
    UserDto toUserDto(FanpageAdminRegisterRequestDto fanpageAdminRegisterRequestDto);


    StudentProfileDto toUserProfileDto(RegisterRequestDto userRegisterRequestDto);
    FanpageAdminProfileDto toFanpageAdminProfileDto(FanpageAdminRegisterRequestDto fanpageAdminRegisterRequestDto);
}
