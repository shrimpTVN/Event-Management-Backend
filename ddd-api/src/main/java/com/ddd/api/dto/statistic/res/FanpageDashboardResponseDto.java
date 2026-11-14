package com.ddd.api.dto.statistic.res;

import java.util.List;

public record FanpageDashboardResponseDto(
        FanpageOverviewStatisticResponseDto overview,
        List<FanpageMonthlyTrendResponseDto> monthlyTrends,
        List<FanpageEventDistributionResponseDto> eventsByType,
        List<FanpageTopEventResponseDto> topEvents
) {}
