package com.ddd.infrastructure.dto.row.fanpage;

public record StudentAbsenceRow(
        String studentId,
        Long userId,
        String fullName,
        String email,
        String avatarUrl,
        long absenceCount
) {}
