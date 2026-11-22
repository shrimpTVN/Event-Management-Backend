package com.ddd.api.dto.statistic.res.admin;

public record AdminAttendanceOverviewResponseDto(
        long totalRegistrations,
        long totalAttendees,
        long absentCount,
        double attendanceRate,
        double absentRate
) {}
