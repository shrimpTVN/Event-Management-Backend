package com.ddd.api.dto.statistic.res.fanpage;

import java.util.List;

public record FanpageDemographicsResponseDto(
        long totalRegistrations,
        List<DemographicItemResponseDto> cohorts,
        List<DemographicItemResponseDto> schools,
        List<DemographicItemResponseDto> majors
) {}
