package com.ddd.application.dto.statistic;

public record FanpageOverviewStatisticDto(
        long totalEvents,
        long totalFollowers,
        long totalMembers,
        long totalRegistrations,
        long totalAttendees,
        double attendanceRate
) {}
