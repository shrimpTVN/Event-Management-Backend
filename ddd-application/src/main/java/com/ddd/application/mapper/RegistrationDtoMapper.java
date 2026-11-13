package com.ddd.application.mapper;

import com.ddd.application.dto.export.AttendeeExportRowDto;
import com.ddd.application.dto.fanpage.AttendeeInfoDto;
import com.ddd.application.dto.registration.RegistrationArrangedInfoDto;
import com.ddd.application.dto.registration.RegistrationInfoDto;
import com.ddd.infrastructure.entity.RegistrationJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {EventDtoMapper.class})
public interface RegistrationDtoMapper {


    @Mapping(target="eventInfo", source="event")
    @Mapping(target="userId", source="user.id")
    @Mapping(target="eventId", source="event.id")
    RegistrationInfoDto toInfoDto(RegistrationJpaEntity entity);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target="firstName", source="user.firstName")
    @Mapping(target="lastName", source="user.lastName")
    @Mapping(target="email", source="user.email")
    @Mapping(target="gender", source="user.gender")
    @Mapping(target="avatarUrl", source="user.avatarUrl")
    @Mapping(target="studentId", source="user.studentProfile.studentId")
    @Mapping(target="KNumber", source="user.studentProfile.KNumber")
    @Mapping(target="majorName", source="user.studentProfile.major.name")
    AttendeeInfoDto toAttendeeInfoDto(RegistrationJpaEntity entity);

    @Mapping(target="index", source="index")
    @Mapping(target="firstName", source="entity.user.firstName")
    @Mapping(target="lastName", source="entity.user.lastName")
    @Mapping(target="email", source="entity.user.email")
    @Mapping(target="gender", source="entity.user.gender")
    @Mapping(target="studentId", source="entity.user.studentProfile.studentId")
    @Mapping(target="majorName", source="entity.user.studentProfile.major.name")
    AttendeeExportRowDto toAttendeeExportRowDto(RegistrationJpaEntity entity, int index);

    @Mapping(target="eventId", source="event.id")
    @Mapping(target="eventName", source="event.name")
    @Mapping(target="eventDescription", source="event.description")
    @Mapping(target="eventBannerUrl", source="event.bannerUrl")
    @Mapping(target="eventDateHappen", source="event.dateHappen")
    @Mapping(target="eventLocation", source="event.location")
    RegistrationArrangedInfoDto toArrangedInfoDto(RegistrationJpaEntity entity);
}
