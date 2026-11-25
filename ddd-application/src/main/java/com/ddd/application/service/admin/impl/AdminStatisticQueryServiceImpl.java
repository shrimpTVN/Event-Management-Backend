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
    public AdminOverviewStatisticDto getOverview(Integer year, Long semesterId, String academicYear) {
        assertSuperAdmin();
        String normalizedYear = normalizeAcademicYear(academicYear);
        var userRow = adminStatisticRepositoryImpl.getUserOverview();
        var fanpageRow = adminStatisticRepositoryImpl.getFanpageOverview();
        var eventRow = adminStatisticRepositoryImpl.getEventOverview(year, semesterId, normalizedYear);
        var attendanceRow = adminStatisticRepositoryImpl.getAttendanceOverview(year, semesterId, normalizedYear);
        return adminStatisticDtoMapper.toOverviewDto(userRow, fanpageRow, eventRow, attendanceRow);
    }

    @Override
    public List<AdminMonthlyTrendDto> getMonthlyTrends(Integer year, Long semesterId, String academicYear) {
        assertSuperAdmin();
        String normalizedYear = normalizeAcademicYear(academicYear);
        Integer resolvedYear = (year != null) ? year : (normalizedYear == null && semesterId == null ? LocalDate.now().getYear() : null);
        var rows = adminStatisticRepositoryImpl.getMonthlyTrends(resolvedYear, semesterId, normalizedYear);
        return adminStatisticDtoMapper.toMonthlyTrendDtoList(rows);
    }

    @Override
    public List<AdminEventTypeDistributionDto> getEventDistribution(Integer year, Long semesterId, String academicYear) {
        assertSuperAdmin();
        String normalizedYear = normalizeAcademicYear(academicYear);
        var rows = adminStatisticRepositoryImpl.getEventDistribution(year, semesterId, normalizedYear);
        long totalEvents = rows.stream().mapToLong(AdminEventTypeDistributionRow::eventCount).sum();
        return adminStatisticDtoMapper.toDistributionDtoList(rows, totalEvents);
    }

    @Override
    public List<AdminTopFanpageDto> getTopFanpages(Integer year, Long semesterId, Integer limit, String academicYear) {
        assertSuperAdmin();
        String normalizedYear = normalizeAcademicYear(academicYear);
        Integer resolvedLimit = (limit != null && limit > 0) ? Math.min(limit, 50) : null;
        var rows = adminStatisticRepositoryImpl.getTopFanpages(year, semesterId, resolvedLimit, normalizedYear);
        return adminStatisticDtoMapper.toTopFanpageDtoList(rows);
    }

    @Override
    public AdminDemographicsDto getDemographics(Integer year, Long semesterId, String academicYear) {
        assertSuperAdmin();
        String normalizedYear = normalizeAcademicYear(academicYear);
        var cohortRows = adminStatisticRepositoryImpl.getCohortDemographics(year, semesterId, normalizedYear);
        var schoolRows = adminStatisticRepositoryImpl.getSchoolDemographics(year, semesterId, normalizedYear);
        var majorRows = adminStatisticRepositoryImpl.getMajorDemographics(year, semesterId, normalizedYear);
        return adminStatisticDtoMapper.toDemographicsDto(cohortRows, schoolRows, majorRows);
    }

    @Override
    public List<AdminMonthlyAbsentRateDto> getMonthlyAbsentRate(Integer year, Long semesterId, String academicYear) {
        assertSuperAdmin();
        String normalizedYear = normalizeAcademicYear(academicYear);
        var rows = adminStatisticRepositoryImpl.getMonthlyAbsentRate(year, semesterId, normalizedYear);
        return adminStatisticDtoMapper.toMonthlyAbsentRateDtoList(rows);
    }

    @Override
    public AdminDashboardDto getDashboard(Integer year, Long semesterId, String academicYear) {
        assertSuperAdmin();
        String normalizedYear = normalizeAcademicYear(academicYear);
        Integer resolvedYear = (year != null) ? year : (normalizedYear == null && semesterId == null ? LocalDate.now().getYear() : null);

        var overview = getOverview(resolvedYear, semesterId, normalizedYear);
        var trends = getMonthlyTrends(resolvedYear, semesterId, normalizedYear);
        var distribution = getEventDistribution(resolvedYear, semesterId, normalizedYear);
        var topFanpages = getTopFanpages(resolvedYear, semesterId, null, normalizedYear);
        var demographics = getDemographics(resolvedYear, semesterId, normalizedYear);
        var absentRates = getMonthlyAbsentRate(resolvedYear, semesterId, normalizedYear);

        return adminStatisticDtoMapper.toDashboardDto(overview, trends, distribution, topFanpages, demographics, absentRates);
    }

    private String normalizeAcademicYear(String academicYear) {
        return (academicYear != null && !academicYear.isBlank()) ? academicYear.trim() : null;
    }

    private void assertSuperAdmin() {
        if (!securityContextPort.hasRole("ADMIN")) {
            throw new UnauthorizedException("Access denied. Only Super Admin can access admin statistics.");
        }
    }
}
