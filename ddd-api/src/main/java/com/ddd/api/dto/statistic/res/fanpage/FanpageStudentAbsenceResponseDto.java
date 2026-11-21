package com.ddd.api.dto.statistic.res.fanpage;

public record FanpageStudentAbsenceResponseDto(
        String studentId,
        Long userId,
        String fullName,
        String email,
        String avatarUrl,
        long absenceCount
) {}
