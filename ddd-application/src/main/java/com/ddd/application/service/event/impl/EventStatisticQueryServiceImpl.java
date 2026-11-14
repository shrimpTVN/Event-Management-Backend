package com.ddd.application.service.event.impl;

import com.ddd.application.dto.statistic.*;
import com.ddd.application.service.event.EventStatisticQueryService;
import com.ddd.domain.exception.AuthException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.infrastructure.dto.row.event.CheckInTimelineRow;
import com.ddd.infrastructure.dto.row.event.DemographicItemRow;
import com.ddd.infrastructure.dto.row.event.EventOverviewRow;
import com.ddd.infrastructure.dto.row.event.EventStatusFunnelRow;
import com.ddd.infrastructure.repository.EventStatisticRepository;
import com.ddd.infrastructure.repository.jpaRepository.EventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventStatisticQueryServiceImpl implements EventStatisticQueryService {

    private final EventStatisticRepository eventStatisticRepositoryImpl;
    private final EventJpaRepository eventJpaRepository;
    private final FanpageMemberRepository fanpageMemberRepository;
    private final SecurityContextPort securityContextPort;

    @Override
    public EventOverviewStatisticDto getOverview(Long fanpageId, Long eventId) {
        assertCallerCanViewEventStatistics(fanpageId, eventId);

        EventOverviewRow row = eventStatisticRepositoryImpl.getOverviewRow(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event statistics not found for id: " + eventId));

        double utilizationRate = row.capacity() == 0 ? 0.0
                : roundRate((double) row.totalRegistrations() / row.capacity() * 100.0);
        double attendanceRate = row.totalRegistrations() == 0 ? 0.0
                : roundRate((double) row.totalAttended() / row.totalRegistrations() * 100.0);

        return new EventOverviewStatisticDto(
                row.capacity(),
                row.maleQuantity(),
                row.femaleQuantity(),
                row.totalRegistrations(),
                utilizationRate,
                row.totalAttended(),
                attendanceRate
        );
    }

    @Override
    public EventStatusFunnelDto getStatusFunnel(Long fanpageId, Long eventId) {
        assertCallerCanViewEventStatistics(fanpageId, eventId);

        EventStatusFunnelRow row = eventStatisticRepositoryImpl.getStatusFunnelRow(eventId);
        return new EventStatusFunnelDto(
                row.registeredCount(),
                row.selfCheckedInCount(),
                row.checkedInCount(),
                row.spendingConfirmCount(),
                row.confirmedCount(),
                row.completedCount(),
                row.absentCount(),
                row.cancelledCount()
        );
    }

    @Override
    public EventDemographicsDto getDemographics(Long fanpageId, Long eventId) {
        assertCallerCanViewEventStatistics(fanpageId, eventId);

        long totalRegistrations = eventStatisticRepositoryImpl.getOverviewRow(eventId)
                .map(EventOverviewRow::totalRegistrations)
                .orElse(0L);

        List<EventDemographicItemDto> cohorts = mapDemographicItems(
                eventStatisticRepositoryImpl.getCohortDemographics(eventId), totalRegistrations);
        List<EventDemographicItemDto> majors = mapDemographicItems(
                eventStatisticRepositoryImpl.getMajorDemographics(eventId), totalRegistrations);
        List<EventDemographicItemDto> genders = mapDemographicItems(
                eventStatisticRepositoryImpl.getGenderDemographics(eventId), totalRegistrations);

        return new EventDemographicsDto(cohorts, majors, genders);
    }

    @Override
    public List<EventCheckInTimelineDto> getCheckInTimeline(Long fanpageId, Long eventId) {
        assertCallerCanViewEventStatistics(fanpageId, eventId);

        List<CheckInTimelineRow> rows = eventStatisticRepositoryImpl.getCheckInTimeline(eventId);
        return rows.stream()
                .map(r -> new EventCheckInTimelineDto(r.timeSlot(), r.checkInCount()))
                .toList();
    }

    @Override
    public EventDashboardStatisticDto getDashboard(Long fanpageId, Long eventId) {
        assertCallerCanViewEventStatistics(fanpageId, eventId);

        EventOverviewStatisticDto overview = getOverview(fanpageId, eventId);
        EventStatusFunnelDto statusFunnel = getStatusFunnel(fanpageId, eventId);
        EventDemographicsDto demographics = getDemographics(fanpageId, eventId);
        List<EventCheckInTimelineDto> timeline = getCheckInTimeline(fanpageId, eventId);

        return new EventDashboardStatisticDto(overview, statusFunnel, demographics, timeline);
    }
    private void assertCallerCanViewEventStatistics(Long fanpageId, Long eventId) {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        if (!eventJpaRepository.existsByIdAndFanpageId(eventId, fanpageId)) {
            throw new ResourceNotFoundException(
                    "Event not found with id " + eventId + " belonging to fanpage " + fanpageId);
        }
        if (securityContextPort.hasRole("ADMIN")) {
            return;
        }
        if (!fanpageMemberRepository.isActiveMember(fanpageId, currentUserId)) {
            throw new AuthException("You do not have permission to view statistics for this event");
        }
    }

    private List<EventDemographicItemDto> mapDemographicItems(List<DemographicItemRow> rows, long totalRegistrations) {
        return rows.stream()
                .map(r -> new EventDemographicItemDto(
                        r.label(),
                        r.studentCount(),
                        totalRegistrations == 0 ? 0.0 : roundRate((double) r.studentCount() / totalRegistrations * 100.0)
                ))
                .toList();
    }

    private double roundRate(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
