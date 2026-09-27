package com.ddd.api.mapper;

import com.ddd.api.dto.eventtype.req.CreateEventTypeRequestDto;
import com.ddd.api.dto.eventtype.req.UpdateEventTypeRequestDto;
import com.ddd.api.dto.eventtype.res.EventTypeResponseDto;
import com.ddd.application.dto.eventtype.EventTypeDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventTypeApiMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    EventTypeDto toDto(CreateEventTypeRequestDto req);

    @Mapping(target = "id", ignore = true)
    EventTypeDto toDto(UpdateEventTypeRequestDto req);

    EventTypeResponseDto toResponseDto(EventTypeDto dto);
}
