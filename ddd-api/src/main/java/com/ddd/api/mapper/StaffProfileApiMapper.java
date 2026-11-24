package com.ddd.api.mapper;

import com.ddd.api.dto.user.req.StaffProfileCreateRequestDto;
import com.ddd.api.dto.user.req.StaffProfileDetailsCreateRequestDto;
import com.ddd.api.dto.user.req.StaffProfileDetailsUpdateRequestDto;
import com.ddd.api.dto.user.req.StaffProfileUpdateRequestDto;
import com.ddd.api.dto.user.res.StaffProfileDetailsResponseDto;
import com.ddd.api.dto.user.res.StaffProfileResponseDto;
import com.ddd.application.dto.user.StaffProfileCreateDto;
import com.ddd.application.dto.user.StaffProfileDetailsCreateDto;
import com.ddd.application.dto.user.StaffProfileDetailsDto;
import com.ddd.application.dto.user.StaffProfileDetailsUpdateDto;
import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.application.dto.user.StaffProfileUpdateDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StaffProfileApiMapper {

    StaffProfileResponseDto toStaffProfileResponseDto(StaffProfileSummaryDto dto);

    StaffProfileDetailsResponseDto toStaffProfileDetailsResponseDto(StaffProfileDetailsDto dto);

    StaffProfileUpdateDto toStaffProfileUpdateDto(StaffProfileUpdateRequestDto requestDto);

    StaffProfileDetailsUpdateDto toStaffProfileDetailsUpdateDto(StaffProfileDetailsUpdateRequestDto requestDto);

    StaffProfileCreateDto toStaffProfileCreateDto(StaffProfileCreateRequestDto requestDto);

    StaffProfileDetailsCreateDto toStaffProfileDetailsCreateDto(StaffProfileDetailsCreateRequestDto requestDto);
}
