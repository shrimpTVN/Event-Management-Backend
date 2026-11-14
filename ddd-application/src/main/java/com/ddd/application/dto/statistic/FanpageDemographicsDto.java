package com.ddd.application.dto.statistic;

import java.util.List;

public record FanpageDemographicsDto(
        long totalRegistrations,
        List<FanpageDemographicItemDto> cohorts,
        List<FanpageDemographicItemDto> schools,
        List<FanpageDemographicItemDto> majors
) {}
