package com.ddd.application.service.event;

import com.ddd.application.dto.statistic.*;

import java.util.List;

public interface EventStatisticQueryService {
    EventOverviewStatisticDto getOverview(Long fanpageId, Long eventId);
    EventStatusFunnelDto getStatusFunnel(Long fanpageId, Long eventId);
    EventDemographicsDto getDemographics(Long fanpageId, Long eventId);
    List<EventCheckInTimelineDto> getCheckInTimeline(Long fanpageId, Long eventId);
    EventDashboardStatisticDto getDashboard(Long fanpageId, Long eventId);
}
