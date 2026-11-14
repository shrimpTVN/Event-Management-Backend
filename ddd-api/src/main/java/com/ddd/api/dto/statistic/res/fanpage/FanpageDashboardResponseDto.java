package com.ddd.api.dto.statistic.res.fanpage;

import java.util.List;

public record FanpageDashboardResponseDto(
        FanpageOverviewStatisticResponseDto overview,
        List<FanpageMonthlyTrendResponseDto> monthlyTrends,
        List<FanpageEventDistributionResponseDto> eventsByType,
        List<FanpageTopEventResponseDto> topEvents,
        FanpageDemographicsResponseDto demographics
) {
    public FanpageDashboardResponseDto(
            FanpageOverviewStatisticResponseDto overview,
            List<FanpageMonthlyTrendResponseDto> monthlyTrends,
            List<FanpageEventDistributionResponseDto> eventsByType,
            List<FanpageTopEventResponseDto> topEvents
    ) {
        this(overview, monthlyTrends, eventsByType, topEvents, null);
    }
}
