package com.ddd.api.mapper;

import com.ddd.api.dto.notification.res.NotificationResponseDto;
import com.ddd.application.dto.notification.NotificationInfoDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationApiMapper {

    NotificationResponseDto toResponseDto(NotificationInfoDto infoDto);
}
