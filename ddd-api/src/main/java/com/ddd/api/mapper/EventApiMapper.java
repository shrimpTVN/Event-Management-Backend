package com.ddd.api.mapper;

import com.ddd.api.dto.event.req.EventCreateRequestDto;
import com.ddd.api.dto.event.req.EventPointRequestDto;
import com.ddd.api.dto.event.res.EventPointResponseDto;
import com.ddd.api.dto.event.res.EventResponseDto;
import com.ddd.application.dto.event.EventCreateDto;
import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.dto.event.EventPointCreateDto;
import com.ddd.application.dto.event.EventPointInfoDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventApiMapper {

    EventCreateDto toCreateDto(EventCreateRequestDto requestDto);

    EventPointCreateDto toEventPointCreateDto(EventPointRequestDto request);

    EventPointResponseDto toEventPointResponseDto(EventPointInfoDto eventPointInfoDto);

    EventResponseDto toResponseDto(EventInfoDto eventInfoDto);
}
