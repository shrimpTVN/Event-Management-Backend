package com.ddd.api.dto.statistic.res.admin;

public record AdminOverviewStatisticResponseDto(
        AdminUserOverviewResponseDto users,
        AdminFanpageOverviewResponseDto fanpages,
        AdminEventOverviewResponseDto events,
        AdminAttendanceOverviewResponseDto attendance
) {}
