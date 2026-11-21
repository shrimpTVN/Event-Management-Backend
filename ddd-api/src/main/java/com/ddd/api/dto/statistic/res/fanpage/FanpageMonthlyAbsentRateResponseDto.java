package com.ddd.api.dto.statistic.res.fanpage;

public record FanpageMonthlyAbsentRateResponseDto(
        int month,
        String monthName,
        long totalRegistrations,
        long absentCount,
        double absentRate
) {}
