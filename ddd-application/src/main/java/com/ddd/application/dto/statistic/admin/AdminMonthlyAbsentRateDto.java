package com.ddd.application.dto.statistic.admin;

public record AdminMonthlyAbsentRateDto(
        int month,
        String monthName,
        long totalRegistrations,
        long absentCount,
        double absentRate
) {}
