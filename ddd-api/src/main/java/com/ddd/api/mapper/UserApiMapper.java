package com.ddd.api.mapper;

import com.ddd.api.dto.user.res.UserResponseDto;
import com.ddd.application.dto.user.UserSummaryDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserApiMapper {
    UserResponseDto toUserResponseDto(UserSummaryDto userSummaryDto);
}
