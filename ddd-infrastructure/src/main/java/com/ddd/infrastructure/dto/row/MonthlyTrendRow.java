package com.ddd.infrastructure.dto.row;

/**
 * Internal JDBC result row for monthly trend aggregate queries.
 */
public record MonthlyTrendRow(
        String monthYear,
        long eventCount,
        long registrationCount,
        long attendeeCount
) {}
