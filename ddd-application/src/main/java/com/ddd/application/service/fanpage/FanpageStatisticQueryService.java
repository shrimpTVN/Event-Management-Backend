package com.ddd.application.service.fanpage;

import com.ddd.application.dto.statistic.*;

import java.util.List;

public interface FanpageStatisticQueryService {
    FanpageOverviewStatisticDto getOverview(Long fanpageId);
    List<FanpageMonthlyTrendDto> getMonthlyTrends(Long fanpageId, Integer year);
    List<FanpageEventDistributionDto> getEventDistribution(Long fanpageId);
    List<FanpageTopEventDto> getTopEvents(Long fanpageId, int limit);
    FanpageDemographicsDto getDemographics(Long fanpageId, Integer year, Long semesterId);
    FanpageDashboardDto getDashboard(Long fanpageId, Integer year, Long semesterId);
    default FanpageDashboardDto getDashboard(Long fanpageId, Integer year) {
        return getDashboard(fanpageId, year, null);
    }
}
