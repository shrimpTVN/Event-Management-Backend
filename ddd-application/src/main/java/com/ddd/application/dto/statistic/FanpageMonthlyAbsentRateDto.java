package com.ddd.application.dto.statistic;

public record FanpageMonthlyAbsentRateDto(
        int month,
        String monthName,
        long totalRegistrations,
        long absentCount,
        double absentRate
) {}
