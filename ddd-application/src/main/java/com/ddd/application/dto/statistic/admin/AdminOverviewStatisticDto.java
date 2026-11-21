package com.ddd.application.dto.statistic.admin;

public record AdminOverviewStatisticDto(
        AdminUserOverviewDto users,
        AdminFanpageOverviewDto fanpages,
        AdminEventOverviewDto events,
        AdminAttendanceOverviewDto attendance
) {}
