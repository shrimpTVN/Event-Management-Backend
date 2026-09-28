package com.ddd.application.mapper;

import com.ddd.application.dto.user.FanpageAdminProfileDto;
import com.ddd.domain.model.FanpageAdminProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FanpageAdminProfileDtoMapper {

    FanpageAdminProfileDto toDto(FanpageAdminProfile fanpageAdminProfile);

    FanpageAdminProfile toEntity(FanpageAdminProfileDto fanpageAdminProfileDto);
}
