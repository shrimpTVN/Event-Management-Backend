package com.ddd.infrastructure.dto.row;

import java.time.Instant;

/**
 * Internal JDBC result row for top-events aggregate queries.
 */
public record TopEventRow(
        Long eventId,
        String eventName,
        String bannerUrl,
        Instant dateHappen,
        int capacity,
        long registrationCount,
        long attendeeCount
) {}
