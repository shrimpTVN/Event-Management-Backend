package com.ddd.infrastructure.dto.row.fanpage;

public record MonthlyAbsentRateRow(
        int month,
        String monthName,
        long totalRegistrations,
        long absentCount,
        double absentRate
) {}
