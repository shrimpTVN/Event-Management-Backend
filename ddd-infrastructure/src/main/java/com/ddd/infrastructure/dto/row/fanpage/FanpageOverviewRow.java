package com.ddd.infrastructure.dto.row.fanpage;

/**
 * Internal JDBC result row for the fanpage overview aggregate query.
 * Not exposed beyond ddd-infrastructure.
 */
public record FanpageOverviewRow(
        long totalEvents,
        long totalFollowers,
        long totalMembers,
        long totalRegistrations,
        long totalAttendees
) {}
