package com.ddd.application.dto.statistic.admin;

public record AdminEventTypeDistributionDto(
        Long eventTypeId,
        String eventTypeName,
        long eventCount,
        double percentage
) {}
