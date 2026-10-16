package com.ddd.application.mapper;

import com.ddd.application.dto.fanpage.AttendeeInfoDto;
import com.ddd.application.dto.registration.RegistrationDto;
import com.ddd.infrastructure.entity.RegistrationJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RegistrationDtoMapper {

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventName", source = "event.name")
    @Mapping(target = "location", source = "event.location")
    @Mapping(target = "dateOpen", source = "event.dateOpen")
    @Mapping(target = "dateClose", source = "event.dateClose")
    @Mapping(target = "dateHappen", source = "event.dateHappen")
    @Mapping(target = "bannerUrl", source = "event.bannerUrl")
    RegistrationDto toDto(RegistrationJpaEntity entity);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target="firstName", source="user.firstName")
    @Mapping(target="lastName", source="user.lastName")
    @Mapping(target="email", source="user.email")
    @Mapping(target="gender", source="user.gender")
    @Mapping(target="avatarUrl", source="user.avatarUrl")
    AttendeeInfoDto toAttendeeInfoDto(RegistrationJpaEntity entity);
}
