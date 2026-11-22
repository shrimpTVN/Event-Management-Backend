package com.ddd.infrastructure.dto.row.admin;

public record AdminMonthlyTrendRow(
        String monthYear,
        long eventCount,
        long registrationCount,
        long attendeeCount,
        long absentCount,
        double absentRate
) {}
