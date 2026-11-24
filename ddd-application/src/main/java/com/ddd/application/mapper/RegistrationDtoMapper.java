package com.ddd.application.mapper;

import com.ddd.application.dto.export.AttendeeExportRowDto;
import com.ddd.application.dto.fanpage.AttendeeInfoDto;
import com.ddd.application.dto.registration.RegistrationArrangedInfoDto;
import com.ddd.application.dto.registration.RegistrationInfoDto;
import com.ddd.application.dto.registration.StudentProfileBasicInfoDto;
import com.ddd.application.dto.registration.StudentRegistrationInfoDto;
import com.ddd.domain.enums.RegistrationStatusEnum;
import com.ddd.domain.model.Registration;
import com.ddd.infrastructure.entity.RegistrationJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.Instant;

@Mapper(componentModel = "spring", uses = {EventDtoMapper.class})
public interface RegistrationDtoMapper {

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "regDate", source = "regDate")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "reason", expression = "java(null)")
    @Mapping(target = "checkInAt", ignore = true)
    @Mapping(target = "checkOutAt", ignore = true)
    @Mapping(target = "checkInEvidenceUrl", ignore = true)
    @Mapping(target = "checkOutEvidenceUrl", ignore = true)
    @Mapping(target = "checkInBy", ignore = true)
    @Mapping(target = "checkOutBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateRegistrationForRegister(@MappingTarget Registration registration, Long userId, Long eventId, Instant regDate, RegistrationStatusEnum status);


    @Mapping(target="eventInfo", source="event")
    @Mapping(target="userId", source="user.id")
    @Mapping(target="eventId", source="event.id")
    RegistrationInfoDto toInfoDto(RegistrationJpaEntity entity);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "studentProfile", source = "user")
    StudentRegistrationInfoDto toStudentRegistrationInfoDto(RegistrationJpaEntity entity);

    @Mapping(target = "email", source = "email")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "gender", source = "gender")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "studentId", source = "studentProfile.studentId")
    @Mapping(target = "kNumber", source = "studentProfile.KNumber")
    @Mapping(target = "majorName", source = "studentProfile.major.name")
    @Mapping(target = "majorCode", source = "studentProfile.major.code")
    StudentProfileBasicInfoDto toStudentProfileBasicInfoDto(UserJpaEntity user);

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
