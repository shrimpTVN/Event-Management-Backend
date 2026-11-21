package com.ddd.application.service.admin;

import com.ddd.application.dto.statistic.admin.*;

import java.util.List;

public interface AdminStatisticQueryService {
    AdminOverviewStatisticDto getOverview(Integer year, Long semesterId);
    List<AdminMonthlyTrendDto> getMonthlyTrends(Integer year, Long semesterId);
    List<AdminEventTypeDistributionDto> getEventDistribution(Integer year, Long semesterId);
    List<AdminTopFanpageDto> getTopFanpages(Integer year, Long semesterId, Integer limit);
    AdminDemographicsDto getDemographics(Integer year, Long semesterId);
    List<AdminMonthlyAbsentRateDto> getMonthlyAbsentRate(Integer year, Long semesterId);
    AdminDashboardDto getDashboard(Integer year, Long semesterId);
}
