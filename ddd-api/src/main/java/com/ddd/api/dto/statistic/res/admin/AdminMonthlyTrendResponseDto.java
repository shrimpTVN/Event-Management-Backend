package com.ddd.api.dto.statistic.res.admin;

public record AdminMonthlyTrendResponseDto(
        String monthYear,
        long eventCount,
        long registrationCount,
        long attendeeCount,
        long absentCount,
        double absentRate
) {}
