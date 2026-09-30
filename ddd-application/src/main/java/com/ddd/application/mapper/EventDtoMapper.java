package com.ddd.application.mapper;

import com.ddd.application.dto.event.EventCreateDto;
import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.dto.event.EventPointCreateDto;
import com.ddd.domain.model.Event;
import com.ddd.domain.model.EventPoint;
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

    EventInfoDto toInfoDto(Event event, String eventTypeName, String criteriaName, String fanpageName);

    @Named("instantToLocalDate")
    default LocalDate mapInstantToLocalDate(Instant instant) {
        if (instant == null) return LocalDate.now();
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }
}
