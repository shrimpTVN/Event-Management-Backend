package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.statistic.*;
import com.ddd.application.mapper.FanpageStatisticDtoMapper;
import com.ddd.application.service.fanpage.FanpageStatisticQueryService;
import com.ddd.domain.enums.FollowingStatusEnum;
import com.ddd.domain.exception.AuthException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.FanpageRepository;
import com.ddd.domain.repository.FanpageStatisticRepository;
import com.ddd.domain.repository.FollowingRepository;
import com.ddd.infrastructure.repository.FanpageStatisticRepositoryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FanpageStatisticQueryServiceImpl implements FanpageStatisticQueryService {

    private final FanpageStatisticRepository fanpageStatisticRepository;
    private final FanpageStatisticRepositoryImpl fanpageStatisticRepositoryImpl;
    private final FollowingRepository followingRepository;
    private final FanpageMemberRepository fanpageMemberRepository;
    private final FanpageRepository fanpageRepository;
    private final SecurityContextPort securityContextPort;
    private final FanpageStatisticDtoMapper fanpageStatisticDtoMapper;

    @Override
    public FanpageOverviewStatisticDto getOverview(Long fanpageId) {
        assertFanpageExists(fanpageId);
        assertCallerIsAdminOfFanpage(fanpageId);

        var row = fanpageStatisticRepositoryImpl.getOverviewRow(fanpageId);
        long totalFollowers = followingRepository.countByFanpageIdAndFolStatus(
                fanpageId, FollowingStatusEnum.FOLLOWING);

        return fanpageStatisticDtoMapper.toOverviewDto(row, totalFollowers);
    }

    @Override
    public List<FanpageMonthlyTrendDto> getMonthlyTrends(Long fanpageId, Integer year) {
        assertFanpageExists(fanpageId);
        assertCallerIsAdminOfFanpage(fanpageId);

        int resolvedYear = (year != null) ? year : LocalDate.now().getYear();
        var rows = fanpageStatisticRepositoryImpl.getMonthlyTrends(fanpageId, resolvedYear);

        return fanpageStatisticDtoMapper.toMonthlyTrendDtoList(rows);
    }

    @Override
    public List<FanpageEventDistributionDto> getEventDistribution(Long fanpageId) {
        assertFanpageExists(fanpageId);
        assertCallerIsAdminOfFanpage(fanpageId);

        long totalEvents = fanpageStatisticRepository.countEventsByFanpageId(fanpageId);
        var rows = fanpageStatisticRepositoryImpl.getEventDistribution(fanpageId);

        return fanpageStatisticDtoMapper.toDistributionDtoList(rows, totalEvents);
    }

    @Override
    public List<FanpageTopEventDto> getTopEvents(Long fanpageId, int limit) {
        assertFanpageExists(fanpageId);
        assertCallerIsAdminOfFanpage(fanpageId);

        var rows = fanpageStatisticRepositoryImpl.getTopEvents(fanpageId, Math.min(limit, 20));

        return fanpageStatisticDtoMapper.toTopEventDtoList(rows);
    }

    @Override
    public FanpageDemographicsDto getDemographics(Long fanpageId, Integer year, Long semesterId) {
        assertFanpageExists(fanpageId);
        assertCallerIsAdminOfFanpage(fanpageId);

        var cohortRows = fanpageStatisticRepositoryImpl.getCohortDemographics(fanpageId, year, semesterId);
        var schoolRows = fanpageStatisticRepositoryImpl.getSchoolDemographics(fanpageId, year, semesterId);
        var majorRows = fanpageStatisticRepositoryImpl.getMajorDemographics(fanpageId, year, semesterId);

        return fanpageStatisticDtoMapper.toDemographicsDto(cohortRows, schoolRows, majorRows);
    }

    @Override
    public FanpageDashboardDto getDashboard(Long fanpageId, Integer year, Long semesterId) {
        assertFanpageExists(fanpageId);
        assertCallerIsAdminOfFanpage(fanpageId);

        int resolvedYear = (year != null) ? year : LocalDate.now().getYear();

        var overviewRow = fanpageStatisticRepositoryImpl.getOverviewRow(fanpageId);
        long totalFollowers = followingRepository.countByFanpageIdAndFolStatus(
                fanpageId, FollowingStatusEnum.FOLLOWING);
        FanpageOverviewStatisticDto overview = fanpageStatisticDtoMapper.toOverviewDto(overviewRow, totalFollowers);

        List<FanpageMonthlyTrendDto> trends = fanpageStatisticDtoMapper.toMonthlyTrendDtoList(
                fanpageStatisticRepositoryImpl.getMonthlyTrends(fanpageId, resolvedYear));

        List<FanpageEventDistributionDto> distribution = fanpageStatisticDtoMapper.toDistributionDtoList(
                fanpageStatisticRepositoryImpl.getEventDistribution(fanpageId), overviewRow.totalEvents());

        List<FanpageTopEventDto> topEvents = fanpageStatisticDtoMapper.toTopEventDtoList(
                fanpageStatisticRepositoryImpl.getTopEvents(fanpageId, 5));

        FanpageDemographicsDto demographics = getDemographics(fanpageId, resolvedYear, semesterId);

        return fanpageStatisticDtoMapper.toDashboardDto(overview, trends, distribution, topEvents, demographics);
    }

    private void assertFanpageExists(Long fanpageId) {
        if (fanpageRepository.findByIdAndIsActive(fanpageId, true).isEmpty()) {
            throw new ResourceNotFoundException("Fanpage not found with id: " + fanpageId);
        }
    }

    private void assertCallerIsAdminOfFanpage(Long fanpageId) {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        if (securityContextPort.hasRole("ADMIN")) return;
        if (!fanpageMemberRepository.isActiveMember(fanpageId, currentUserId)) {
            throw new AuthException("You do not have permission to view statistics for this fanpage. Only the fanpage members can access this information.");
        }
    }
}
