package com.ddd.api.dto.statistic.res;

public record FanpageOverviewStatisticResponseDto(
        long totalEvents,
        long totalFollowers,
        long totalMembers,
        long totalRegistrations,
        long totalAttendees,
        double attendanceRate
) {}
