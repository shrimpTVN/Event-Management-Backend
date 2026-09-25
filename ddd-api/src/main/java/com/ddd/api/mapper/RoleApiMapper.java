package com.ddd.api.mapper;

import com.ddd.api.dto.role.req.RoleRequestDto;
import com.ddd.api.dto.role.res.RoleResponseDto;
import com.ddd.application.dto.role.RoleDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleApiMapper {
    RoleDto toDto(RoleRequestDto req);
    RoleResponseDto toResponseDto(RoleDto dto);
}
