package com.ddd.api.dto.statistic.res.admin;

public record AdminMonthlyAbsentRateResponseDto(
        int month,
        String monthName,
        long totalRegistrations,
        long absentCount,
        double absentRate
) {}
