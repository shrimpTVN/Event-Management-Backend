package com.ddd.api.dto.statistic.res.admin;

public record AdminEventTypeDistributionResponseDto(
        Long eventTypeId,
        String eventTypeName,
        long eventCount,
        double percentage
) {}
