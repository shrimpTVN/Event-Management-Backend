package com.ddd.api.dto.statistic.res.admin;

public record AdminUserOverviewResponseDto(
        long totalUsers,
        long totalStudents,
        long totalStaff,
        long totalActiveUsers
) {}
