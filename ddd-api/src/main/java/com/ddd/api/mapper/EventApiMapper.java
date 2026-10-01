package com.ddd.api.mapper;

import com.ddd.api.dto.event.req.EventCreateRequestDto;
import com.ddd.api.dto.event.req.EventPointRequestDto;
import com.ddd.api.dto.event.req.EventUpdateRequestDto;
import com.ddd.api.dto.event.res.EventPointResponseDto;
import com.ddd.api.dto.event.res.EventResponseDto;
import com.ddd.application.dto.event.EventCreateDto;
import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.dto.event.EventPointCreateDto;
import com.ddd.application.dto.event.EventPointInfoDto;
import com.ddd.application.dto.event.EventUpdateDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {SemesterApiMapper.class})
public interface EventApiMapper {

    EventCreateDto toCreateDto(EventCreateRequestDto requestDto);

    EventUpdateDto toUpdateDto(EventUpdateRequestDto requestDto);

    EventPointCreateDto toEventPointCreateDto(EventPointRequestDto request);

    EventPointResponseDto toEventPointResponseDto(EventPointInfoDto eventPointInfoDto);

    EventResponseDto toResponseDto(EventInfoDto eventInfoDto);
}
