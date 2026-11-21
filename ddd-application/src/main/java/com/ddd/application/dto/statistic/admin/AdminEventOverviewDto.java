package com.ddd.application.dto.statistic.admin;

public record AdminEventOverviewDto(
        long totalEvents,
        long publishedEvents,
        long completedEvents,
        long cancelledEvents
) {}
