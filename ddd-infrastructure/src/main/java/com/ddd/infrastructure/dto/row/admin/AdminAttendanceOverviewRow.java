package com.ddd.infrastructure.dto.row.admin;

public record AdminAttendanceOverviewRow(
        long totalRegistrations,
        long totalAttendees,
        long absentCount,
        double attendanceRate,
        double absentRate
) {}
