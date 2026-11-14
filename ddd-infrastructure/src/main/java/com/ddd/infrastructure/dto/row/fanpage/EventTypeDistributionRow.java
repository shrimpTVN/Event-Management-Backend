package com.ddd.infrastructure.dto.row.fanpage;

/**
 * Internal JDBC result row for event-type distribution queries.
 */
public record EventTypeDistributionRow(
        String eventTypeName,
        long eventCount
) {}
