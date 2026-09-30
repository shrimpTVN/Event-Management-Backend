package com.ddd.application.mapper;

import com.ddd.application.dto.event.EventCreateDto;
import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.dto.event.EventPointCreateDto;
import com.ddd.application.dto.event.EventPointInfoDto;
import com.ddd.domain.model.Event;
import com.ddd.domain.model.EventPoint;
import com.ddd.infrastructure.entity.EventJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Mapper(componentModel = "spring")
public interface EventDtoMapper {

    Event toEvent(EventCreateDto dto);
    List<EventPoint> toEventPoints(List<EventPointCreateDto> eventPointCreateDtos);
    EventPointInfoDto toEventPointInfoDto(EventPoint eventPoint);

    @Mapping(target = "eventTypeName", source = "entity.eventType.name")
    @Mapping(target = "criteriaName", source = "entity.criteria.name")
    @Mapping(target = "fanpageName", source = "entity.fanpage.name")
    @Mapping(target = "fanpageId", source = "entity.fanpage.id")
    @Mapping(target = "isActive", source = "entity.isActive")
    @Mapping(target = "dateOpen", source = "entity.dateOpen", qualifiedByName = "instantToLocalDate")
    @Mapping(target = "dateClose", source = "entity.dateClose", qualifiedByName = "instantToLocalDate")
    @Mapping(target = "dateHappen", source = "entity.dateHappen", qualifiedByName = "instantToLocalDate")
    @Mapping(target = "eventPoints", source = "eventPoints")
    @Mapping(target = "status", source = "entity.status")
    EventInfoDto toInfoDto(EventJpaEntity entity, List<EventPointInfoDto> eventPoints);

    @Named("instantToLocalDate")
    default LocalDate mapInstantToLocalDate(Instant instant) {
        if (instant == null) return LocalDate.now();
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }
}
