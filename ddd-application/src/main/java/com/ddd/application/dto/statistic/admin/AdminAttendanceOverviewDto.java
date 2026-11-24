package com.ddd.application.dto.statistic.admin;

public record AdminAttendanceOverviewDto(
        long totalRegistrations,
        long totalAttendees,
        long absentCount,
        double attendanceRate,
        double absentRate
) {}
