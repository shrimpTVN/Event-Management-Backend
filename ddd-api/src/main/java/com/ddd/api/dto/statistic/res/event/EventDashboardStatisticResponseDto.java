package com.ddd.api.dto.statistic.res.event;

import java.util.List;

public record EventDashboardStatisticResponseDto(
    EventOverviewStatisticResponseDto overview,
    EventStatusFunnelResponseDto statusFunnel,
    EventDemographicsResponseDto demographics,
    List<EventCheckInTimelineResponseDto> checkInTimeline
) {}

