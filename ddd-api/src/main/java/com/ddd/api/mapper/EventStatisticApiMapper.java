package com.ddd.api.mapper;

import com.ddd.api.dto.statistic.res.event.*;
import com.ddd.application.dto.statistic.*;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventStatisticApiMapper {
    EventOverviewStatisticResponseDto toResponseDto(EventOverviewStatisticDto dto);
    EventStatusFunnelResponseDto toResponseDto(EventStatusFunnelDto dto);
    EventDemographicsResponseDto toResponseDto(EventDemographicsDto dto);
    EventCheckInTimelineResponseDto toResponseDto(EventCheckInTimelineDto dto);
    List<EventCheckInTimelineResponseDto> toTimelineResponseList(List<EventCheckInTimelineDto> dtos);
    EventDashboardStatisticResponseDto toResponseDto(EventDashboardStatisticDto dto);
}