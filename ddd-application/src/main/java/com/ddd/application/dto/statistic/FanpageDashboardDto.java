package com.ddd.application.dto.statistic;

import java.util.List;

public record FanpageDashboardDto(
        FanpageOverviewStatisticDto overview,
        List<FanpageMonthlyTrendDto> monthlyTrends,
        List<FanpageEventDistributionDto> eventsByType,
        List<FanpageTopEventDto> topEvents,
        FanpageDemographicsDto demographics
) {
    public FanpageDashboardDto(
            FanpageOverviewStatisticDto overview,
            List<FanpageMonthlyTrendDto> monthlyTrends,
            List<FanpageEventDistributionDto> eventsByType,
            List<FanpageTopEventDto> topEvents
    ) {
        this(overview, monthlyTrends, eventsByType, topEvents, null);
    }
}
