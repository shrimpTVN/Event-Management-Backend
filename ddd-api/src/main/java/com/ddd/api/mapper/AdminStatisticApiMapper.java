package com.ddd.api.mapper;

import com.ddd.api.dto.statistic.res.admin.*;
import com.ddd.application.dto.statistic.admin.*;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminStatisticApiMapper {

    AdminUserOverviewResponseDto toResponseDto(AdminUserOverviewDto dto);

    AdminFanpageOverviewResponseDto toResponseDto(AdminFanpageOverviewDto dto);

    AdminEventOverviewResponseDto toResponseDto(AdminEventOverviewDto dto);

    AdminAttendanceOverviewResponseDto toResponseDto(AdminAttendanceOverviewDto dto);

    AdminOverviewStatisticResponseDto toResponseDto(AdminOverviewStatisticDto dto);

    AdminMonthlyTrendResponseDto toResponseDto(AdminMonthlyTrendDto dto);

    List<AdminMonthlyTrendResponseDto> toMonthlyTrendResponseDtoList(List<AdminMonthlyTrendDto> dtos);

    AdminEventTypeDistributionResponseDto toResponseDto(AdminEventTypeDistributionDto dto);

    List<AdminEventTypeDistributionResponseDto> toEventTypeDistributionResponseDtoList(List<AdminEventTypeDistributionDto> dtos);

    AdminTopFanpageResponseDto toResponseDto(AdminTopFanpageDto dto);

    List<AdminTopFanpageResponseDto> toTopFanpageResponseDtoList(List<AdminTopFanpageDto> dtos);

    AdminDemographicItemResponseDto toResponseDto(AdminDemographicItemDto dto);

    AdminDemographicsResponseDto toResponseDto(AdminDemographicsDto dto);

    AdminMonthlyAbsentRateResponseDto toResponseDto(AdminMonthlyAbsentRateDto dto);

    List<AdminMonthlyAbsentRateResponseDto> toMonthlyAbsentRateResponseDtoList(List<AdminMonthlyAbsentRateDto> dtos);

    AdminDashboardResponseDto toResponseDto(AdminDashboardDto dto);
}
