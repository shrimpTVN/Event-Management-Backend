package com.ddd.api.dto.statistic.res.event;

import com.ddd.api.dto.statistic.res.fanpage.DemographicItemResponseDto;

import java.util.List;

public record EventDemographicsResponseDto(
        List<DemographicItemResponseDto> cohorts,
        List<DemographicItemResponseDto> majors,
        List<DemographicItemResponseDto> genders
) {
}
