package com.ddd.api.dto.statistic.res.admin;

import java.util.List;

public record AdminDashboardResponseDto(
        AdminOverviewStatisticResponseDto overview,
        List<AdminMonthlyTrendResponseDto> monthlyTrends,
        List<AdminEventTypeDistributionResponseDto> eventDistribution,
        List<AdminTopFanpageResponseDto> topFanpages,
        AdminDemographicsResponseDto demographics,
        List<AdminMonthlyAbsentRateResponseDto> monthlyAbsentRates
) {}
