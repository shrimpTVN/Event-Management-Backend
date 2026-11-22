package com.ddd.infrastructure.dto.row.admin;

public record AdminEventOverviewRow(
        long totalEvents,
        long publishedEvents,
        long completedEvents,
        long cancelledEvents
) {}
