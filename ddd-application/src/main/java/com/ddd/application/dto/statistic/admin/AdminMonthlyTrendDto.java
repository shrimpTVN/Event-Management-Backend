package com.ddd.application.dto.statistic.admin;

public record AdminMonthlyTrendDto(
        String monthYear,
        long eventCount,
        long registrationCount,
        long attendeeCount,
        long absentCount,
        double absentRate
) {}
