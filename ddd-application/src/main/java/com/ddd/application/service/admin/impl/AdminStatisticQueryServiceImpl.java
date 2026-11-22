package com.ddd.application.service.admin.impl;

import com.ddd.application.dto.statistic.admin.*;
import com.ddd.application.mapper.AdminStatisticDtoMapper;
import com.ddd.application.service.admin.AdminStatisticQueryService;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.dto.row.admin.AdminEventTypeDistributionRow;
import com.ddd.infrastructure.repository.AdminStatisticRepositoryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatisticQueryServiceImpl implements AdminStatisticQueryService {

    private final AdminStatisticRepositoryImpl adminStatisticRepositoryImpl;
    private final AdminStatisticDtoMapper adminStatisticDtoMapper;
    private final SecurityContextPort securityContextPort;

    @Override
    public AdminOverviewStatisticDto getOverview(Integer year, Long semesterId) {
        assertSuperAdmin();
        var userRow = adminStatisticRepositoryImpl.getUserOverview();
        var fanpageRow = adminStatisticRepositoryImpl.getFanpageOverview();
        var eventRow = adminStatisticRepositoryImpl.getEventOverview(year, semesterId);
        var attendanceRow = adminStatisticRepositoryImpl.getAttendanceOverview(year, semesterId);
        return adminStatisticDtoMapper.toOverviewDto(userRow, fanpageRow, eventRow, attendanceRow);
    }

    @Override
    public List<AdminMonthlyTrendDto> getMonthlyTrends(Integer year, Long semesterId) {
        assertSuperAdmin();
        int resolvedYear = (year != null) ? year : LocalDate.now().getYear();
        var rows = adminStatisticRepositoryImpl.getMonthlyTrends(resolvedYear, semesterId);
        return adminStatisticDtoMapper.toMonthlyTrendDtoList(rows);
    }

    @Override
    public List<AdminEventTypeDistributionDto> getEventDistribution(Integer year, Long semesterId) {
        assertSuperAdmin();
        var rows = adminStatisticRepositoryImpl.getEventDistribution(year, semesterId);
        long totalEvents = rows.stream().mapToLong(AdminEventTypeDistributionRow::eventCount).sum();
        return adminStatisticDtoMapper.toDistributionDtoList(rows, totalEvents);
    }

    @Override
    public List<AdminTopFanpageDto> getTopFanpages(Integer year, Long semesterId, Integer limit) {
        assertSuperAdmin();
        int resolvedLimit = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;
        var rows = adminStatisticRepositoryImpl.getTopFanpages(year, semesterId, resolvedLimit);
        return adminStatisticDtoMapper.toTopFanpageDtoList(rows);
    }

    @Override
    public AdminDemographicsDto getDemographics(Integer year, Long semesterId) {
        assertSuperAdmin();
        var cohortRows = adminStatisticRepositoryImpl.getCohortDemographics(year, semesterId);
        var schoolRows = adminStatisticRepositoryImpl.getSchoolDemographics(year, semesterId);
        var majorRows = adminStatisticRepositoryImpl.getMajorDemographics(year, semesterId);
        return adminStatisticDtoMapper.toDemographicsDto(cohortRows, schoolRows, majorRows);
    }

    @Override
    public List<AdminMonthlyAbsentRateDto> getMonthlyAbsentRate(Integer year, Long semesterId) {
        assertSuperAdmin();
        var rows = adminStatisticRepositoryImpl.getMonthlyAbsentRate(year, semesterId);
        return adminStatisticDtoMapper.toMonthlyAbsentRateDtoList(rows);
    }

    @Override
    public AdminDashboardDto getDashboard(Integer year, Long semesterId) {
        assertSuperAdmin();
        int resolvedYear = (year != null) ? year : LocalDate.now().getYear();

        var overview = getOverview(resolvedYear, semesterId);
        var trends = getMonthlyTrends(resolvedYear, semesterId);
        var distribution = getEventDistribution(resolvedYear, semesterId);
        var topFanpages = getTopFanpages(resolvedYear, semesterId, 10);
        var demographics = getDemographics(resolvedYear, semesterId);
        var absentRates = getMonthlyAbsentRate(resolvedYear, semesterId);

        return adminStatisticDtoMapper.toDashboardDto(overview, trends, distribution, topFanpages, demographics, absentRates);
    }

    private void assertSuperAdmin() {
        if (!securityContextPort.hasRole("ADMIN")) {
            throw new UnauthorizedException("Access denied. Only Super Admin can access admin statistics.");
        }
    }
}
