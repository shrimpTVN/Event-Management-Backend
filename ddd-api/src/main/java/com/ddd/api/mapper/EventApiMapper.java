package com.ddd.api.mapper;

import com.ddd.api.dto.event.req.EventCreateRequestDto;
import com.ddd.api.dto.event.req.EventFilterRequestDto;
import com.ddd.api.dto.event.req.EventPointRequestDto;
import com.ddd.api.dto.event.req.EventUpdateRequestDto;
import com.ddd.api.dto.event.res.EventPointResponseDto;
import com.ddd.api.dto.event.res.EventRecommendationResponseDto;
import com.ddd.api.dto.event.res.EventResponseDto;
import com.ddd.api.dto.event.res.PointCategoryGainResponseDto;
import com.ddd.api.dto.event.res.StoredEventStatusResponseDto;
import com.ddd.application.dto.event.*;
import com.ddd.application.dto.recommendation.EventRecommendationDto;
import com.ddd.application.dto.recommendation.PointCategoryGainDto;
import com.ddd.api.dto.event.req.ViewedEventsRequestDto;
import com.ddd.application.dto.recommendation.ViewedEventsDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring", uses = {SemesterApiMapper.class})
public interface EventApiMapper {

    ViewedEventsDto toViewedEventsDto(ViewedEventsRequestDto request);

    EventCreateDto toCreateDto(EventCreateRequestDto requestDto);

    EventUpdateDto toUpdateDto(EventUpdateRequestDto requestDto);

    EventPointCreateDto toEventPointCreateDto(EventPointRequestDto request);

    EventPointResponseDto toEventPointResponseDto(EventPointInfoDto eventPointInfoDto);

    EventResponseDto toResponseDto(EventInfoDto event);

    StoredEventStatusResponseDto toStoredStatusResponseDto(StoredEventStatusDto dto);

    EventSearchCriteriaDto toEventSearchCriteriaDto(EventFilterRequestDto request);

    @Mapping(target = "explain", source = "aiExplanation")
    EventRecommendationResponseDto toRecommendationResponseDto(EventRecommendationDto dto);

    PointCategoryGainResponseDto toPointCategoryGainResponseDto(PointCategoryGainDto dto);
}
