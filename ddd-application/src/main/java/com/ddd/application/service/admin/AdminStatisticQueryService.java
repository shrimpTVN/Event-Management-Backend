package com.ddd.application.service.admin;

import com.ddd.application.dto.statistic.admin.*;

import java.util.List;

public interface AdminStatisticQueryService {
    AdminOverviewStatisticDto getOverview(Integer year, Long semesterId, String academicYear);
    List<AdminMonthlyTrendDto> getMonthlyTrends(Integer year, Long semesterId, String academicYear);
    List<AdminEventTypeDistributionDto> getEventDistribution(Integer year, Long semesterId, String academicYear);
    List<AdminTopFanpageDto> getTopFanpages(Integer year, Long semesterId, Integer limit, String academicYear);
    AdminDemographicsDto getDemographics(Integer year, Long semesterId, String academicYear);
    List<AdminMonthlyAbsentRateDto> getMonthlyAbsentRate(Integer year, Long semesterId, String academicYear);
    AdminDashboardDto getDashboard(Integer year, Long semesterId, String academicYear);
}
