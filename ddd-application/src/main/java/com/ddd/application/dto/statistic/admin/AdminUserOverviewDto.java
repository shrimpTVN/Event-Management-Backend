package com.ddd.application.dto.statistic.admin;

public record AdminUserOverviewDto(
        long totalUsers,
        long totalStudents,
        long totalStaff,
        long totalActiveUsers
) {}
