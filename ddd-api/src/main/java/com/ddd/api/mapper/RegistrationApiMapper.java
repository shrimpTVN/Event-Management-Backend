package com.ddd.api.mapper;

import com.ddd.api.dto.registration.res.RegistrationResponseDto;
import com.ddd.application.dto.registration.RegistrationDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RegistrationApiMapper {
    RegistrationDto toDto(RegistrationResponseDto registrationResponseDto);
    RegistrationResponseDto toResponseDto(RegistrationDto registrationDto);
}
