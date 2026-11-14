package com.ddd.infrastructure.dto.row;

/**
 * Internal JDBC result row for event-type distribution queries.
 */
public record EventTypeDistributionRow(
        String eventTypeName,
        long eventCount
) {}
