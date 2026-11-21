package com.ddd.application.dto.statistic;

import java.util.List;

public record EventDashboardStatisticDto(
        EventOverviewStatisticDto overview,
        EventStatusFunnelDto statusFunnel,
        EventDemographicsDto demographics,
        List<EventCheckInTimelineDto> checkInTimeline
) {
}
