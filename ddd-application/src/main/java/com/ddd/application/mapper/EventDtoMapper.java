package com.ddd.application.mapper;

import com.ddd.application.dto.SemesterDto;
import com.ddd.application.dto.event.EventCreateDto;
import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.dto.event.EventPointCreateDto;
import com.ddd.application.dto.event.EventPointInfoDto;
import com.ddd.domain.model.Event;
import com.ddd.domain.model.EventPoint;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.entity.EventPointJpaEntity;
import com.ddd.application.dto.event.EventUpdateDto;
import com.ddd.infrastructure.entity.SemesterJpaEntity;
import org.mapstruct.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Mapper(componentModel = "spring")
public interface EventDtoMapper {
    // Mapping for Semester
    SemesterDto toSemesterDto(SemesterJpaEntity semester);


    // Mapping for EventPoint
    List<EventPoint> toEventPoints(List<EventPointCreateDto> eventPointCreateDtos);
    EventPointInfoDto toEventPointInfoDto(EventPoint eventPoint);

    @Mapping(target = "pointCategoryId", source = "pointCategory.id")
    @Mapping(target = "eventId", source = "event.id")
    EventPointInfoDto toEventPointInfoDto(EventPointJpaEntity eventPoint);



    // Mapping for Event
    Event toEvent(EventCreateDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fanpageId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEventFromDto(@MappingTarget Event event, EventUpdateDto dto);

    @Mapping(target = "eventTypeName", source = "eventType.name")
    @Mapping(target = "criteriaName", source = "criteria.name")
    @Mapping(target = "fanpageName", source = "fanpage.name")
    @Mapping(target = "fanpageId", source = "fanpage.id")
    @Mapping(target = "semester", source = "semester")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "dateOpen", source = "dateOpen", qualifiedByName = "instantToLocalDate")
    @Mapping(target = "dateClose", source = "dateClose", qualifiedByName = "instantToLocalDate")
    @Mapping(target = "dateHappen", source = "dateHappen", qualifiedByName = "instantToLocalDate")
    @Mapping(target = "eventPoints", source = "eventPoints")
    @Mapping(target = "status", source = "status")
    EventInfoDto toInfoDto(EventJpaEntity entity);

    @Named("instantToLocalDate")
    default LocalDate mapInstantToLocalDate(Instant instant) {
        if (instant == null) return LocalDate.now();
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }
}
