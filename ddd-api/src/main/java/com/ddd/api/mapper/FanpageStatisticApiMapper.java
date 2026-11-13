package com.ddd.api.mapper;

import com.ddd.api.dto.statistic.res.*;
import com.ddd.application.dto.statistic.*;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FanpageStatisticApiMapper {
    FanpageOverviewStatisticResponseDto toResponseDto(FanpageOverviewStatisticDto dto);
    FanpageMonthlyTrendResponseDto toResponseDto(FanpageMonthlyTrendDto dto);
    FanpageEventDistributionResponseDto toResponseDto(FanpageEventDistributionDto dto);
    FanpageTopEventResponseDto toResponseDto(FanpageTopEventDto dto);
    FanpageDashboardResponseDto toResponseDto(FanpageDashboardDto dto);

    List<FanpageMonthlyTrendResponseDto> toMonthlyTrendResponseList(List<FanpageMonthlyTrendDto> dtos);
    List<FanpageEventDistributionResponseDto> toDistributionResponseList(List<FanpageEventDistributionDto> dtos);
    List<FanpageTopEventResponseDto> toTopEventResponseList(List<FanpageTopEventDto> dtos);
}
