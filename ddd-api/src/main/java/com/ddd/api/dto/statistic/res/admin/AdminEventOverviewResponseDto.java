package com.ddd.api.dto.statistic.res.admin;

public record AdminEventOverviewResponseDto(
        long totalEvents,
        long publishedEvents,
        long completedEvents,
        long cancelledEvents
) {}
