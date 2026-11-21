package com.ddd.application.dto.statistic;

public record FanpageStudentAbsenceDto(
        String studentId,
        Long userId,
        String fullName,
        String email,
        String avatarUrl,
        long absenceCount
) {}
