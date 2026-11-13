package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.statistic.*;
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
import com.ddd.infrastructure.dto.row.EventTypeDistributionRow;
import com.ddd.infrastructure.dto.row.MonthlyTrendRow;
import com.ddd.infrastructure.dto.row.TopEventRow;
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

    @Override
    public FanpageOverviewStatisticDto getOverview(Long fanpageId) {
        assertFanpageExists(fanpageId);
        assertCallerIsAdminOfFanpage(fanpageId);

        var row = fanpageStatisticRepositoryImpl.getOverviewRow(fanpageId);

        long totalFollowers = followingRepository.countByFanpageIdAndFolStatus(
                fanpageId, FollowingStatusEnum.FOLLOWING);

        double attendanceRate = row.totalRegistrations() == 0 ? 0.0
                : Math.round((double) row.totalAttendees() / row.totalRegistrations() * 10000.0) / 100.0;

        return new FanpageOverviewStatisticDto(
                row.totalEvents(),
                totalFollowers,
                row.totalMembers(),
                row.totalRegistrations(),
                row.totalAttendees(),
                attendanceRate
        );
    }

    @Override
    public List<FanpageMonthlyTrendDto> getMonthlyTrends(Long fanpageId, Integer year) {
        assertFanpageExists(fanpageId);
        assertCallerIsAdminOfFanpage(fanpageId);

        int resolvedYear = (year != null) ? year : LocalDate.now().getYear();
        List<MonthlyTrendRow> rows = fanpageStatisticRepositoryImpl.getMonthlyTrends(fanpageId, resolvedYear);

        return rows.stream()
                .map(r -> new FanpageMonthlyTrendDto(
                        r.monthYear(), r.eventCount(), r.registrationCount(), r.attendeeCount()))
                .toList();
    }

    @Override
    public List<FanpageEventDistributionDto> getEventDistribution(Long fanpageId) {
        assertFanpageExists(fanpageId);
        assertCallerIsAdminOfFanpage(fanpageId);

        long totalEvents = fanpageStatisticRepository.countEventsByFanpageId(fanpageId);
        List<EventTypeDistributionRow> rows = fanpageStatisticRepositoryImpl.getEventDistribution(fanpageId);

        return rows.stream()
                .map(r -> new FanpageEventDistributionDto(
                        r.eventTypeName(),
                        r.eventCount(),
                        totalEvents == 0 ? 0.0
                                : Math.round((double) r.eventCount() / totalEvents * 10000.0) / 100.0))
                .toList();
    }

    @Override
    public List<FanpageTopEventDto> getTopEvents(Long fanpageId, int limit) {
        assertFanpageExists(fanpageId);
        assertCallerIsAdminOfFanpage(fanpageId);

        List<TopEventRow> rows = fanpageStatisticRepositoryImpl.getTopEvents(fanpageId, Math.min(limit, 20));

        return rows.stream()
                .map(r -> new FanpageTopEventDto(
                        r.eventId(),
                        r.eventName(),
                        r.bannerUrl(),
                        r.dateHappen(),
                        r.capacity(),
                        r.registrationCount(),
                        r.attendeeCount(),
                        r.registrationCount() == 0 ? 0.0
                                : Math.round((double) r.attendeeCount() / r.registrationCount() * 10000.0) / 100.0))
                .toList();
    }

    @Override
    public FanpageDashboardDto getDashboard(Long fanpageId, Integer year) {
        assertFanpageExists(fanpageId);
        assertCallerIsAdminOfFanpage(fanpageId);

        int resolvedYear = (year != null) ? year : LocalDate.now().getYear();

        // Execute all aggregates — guards already passed above, skip re-checking in sub-calls
        var row = fanpageStatisticRepositoryImpl.getOverviewRow(fanpageId);
        long totalFollowers = followingRepository.countByFanpageIdAndFolStatus(
                fanpageId, FollowingStatusEnum.FOLLOWING);
        double attendanceRate = row.totalRegistrations() == 0 ? 0.0
                : Math.round((double) row.totalAttendees() / row.totalRegistrations() * 10000.0) / 100.0;

        FanpageOverviewStatisticDto overview = new FanpageOverviewStatisticDto(
                row.totalEvents(), totalFollowers, row.totalMembers(),
                row.totalRegistrations(), row.totalAttendees(), attendanceRate);

        List<FanpageMonthlyTrendDto> trends = fanpageStatisticRepositoryImpl
                .getMonthlyTrends(fanpageId, resolvedYear)
                .stream()
                .map(r -> new FanpageMonthlyTrendDto(
                        r.monthYear(), r.eventCount(), r.registrationCount(), r.attendeeCount()))
                .toList();

        long totalEvents = row.totalEvents();
        List<FanpageEventDistributionDto> distribution = fanpageStatisticRepositoryImpl
                .getEventDistribution(fanpageId)
                .stream()
                .map(r -> new FanpageEventDistributionDto(
                        r.eventTypeName(), r.eventCount(),
                        totalEvents == 0 ? 0.0
                                : Math.round((double) r.eventCount() / totalEvents * 10000.0) / 100.0))
                .toList();

        List<FanpageTopEventDto> topEvents = fanpageStatisticRepositoryImpl
                .getTopEvents(fanpageId, 5)
                .stream()
                .map(r -> new FanpageTopEventDto(
                        r.eventId(), r.eventName(), r.bannerUrl(), r.dateHappen(), r.capacity(),
                        r.registrationCount(), r.attendeeCount(),
                        r.registrationCount() == 0 ? 0.0
                                : Math.round((double) r.attendeeCount() / r.registrationCount() * 10000.0) / 100.0))
                .toList();

        return new FanpageDashboardDto(overview, trends, distribution, topEvents);
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
