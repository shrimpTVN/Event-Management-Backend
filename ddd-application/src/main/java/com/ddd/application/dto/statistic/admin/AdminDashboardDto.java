package com.ddd.application.dto.statistic.admin;

import java.util.List;

public record AdminDashboardDto(
        AdminOverviewStatisticDto overview,
        List<AdminMonthlyTrendDto> monthlyTrends,
        List<AdminEventTypeDistributionDto> eventDistribution,
        List<AdminTopFanpageDto> topFanpages,
        AdminDemographicsDto demographics,
        List<AdminMonthlyAbsentRateDto> monthlyAbsentRates
) {}
