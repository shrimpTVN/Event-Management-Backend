package com.ddd.infrastructure.dto.row.admin;

public record AdminEventTypeDistributionRow(
        Long eventTypeId,
        String eventTypeName,
        long eventCount
) {}
