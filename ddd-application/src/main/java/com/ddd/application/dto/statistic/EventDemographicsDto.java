package com.ddd.application.dto.statistic;

import java.util.List;

public record EventDemographicsDto(
        List<EventDemographicItemDto> cohorts,
        List<EventDemographicItemDto> majors,
        List<EventDemographicItemDto> genders
) {
}
