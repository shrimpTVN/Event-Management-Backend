package com.ddd.application.mapper;

import com.ddd.application.dto.notification.CreateNotificationDto;
import com.ddd.application.dto.notification.NotificationInfoDto;
import com.ddd.domain.model.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationDtoMapper {

    NotificationInfoDto toInfoDto(Notification notification);
    Notification toModel(CreateNotificationDto dto);
}
