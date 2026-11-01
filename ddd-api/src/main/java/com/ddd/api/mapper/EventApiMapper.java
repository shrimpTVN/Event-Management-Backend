package com.ddd.api.mapper;

import com.ddd.api.dto.event.req.EventCreateRequestDto;
import com.ddd.api.dto.event.req.EventFilterRequestDto;
import com.ddd.api.dto.event.req.EventPointRequestDto;
import com.ddd.api.dto.event.req.EventUpdateRequestDto;
import com.ddd.api.dto.event.res.EventPointResponseDto;
import com.ddd.api.dto.event.res.EventResponseDto;
import com.ddd.application.dto.event.*;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring", uses = {SemesterApiMapper.class})
public interface EventApiMapper {

    EventCreateDto toCreateDto(EventCreateRequestDto requestDto);

    EventUpdateDto toUpdateDto(EventUpdateRequestDto requestDto);

    EventPointCreateDto toEventPointCreateDto(EventPointRequestDto request);

    EventPointResponseDto toEventPointResponseDto(EventPointInfoDto eventPointInfoDto);

    EventResponseDto toResponseDto(EventInfoDto eventInfoDto);

    com.ddd.api.dto.event.res.StoredEventStatusResponseDto toStoredStatusResponseDto(StoredEventStatusDto dto);

    EventSearchCriteriaDto toEventSearchCriteriaDto(EventFilterRequestDto request);
}
