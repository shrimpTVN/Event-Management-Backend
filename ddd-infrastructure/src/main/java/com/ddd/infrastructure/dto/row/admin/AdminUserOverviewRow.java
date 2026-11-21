package com.ddd.infrastructure.dto.row.admin;

public record AdminUserOverviewRow(
        long totalUsers,
        long totalStudents,
        long totalStaff,
        long totalActiveUsers
) {}
