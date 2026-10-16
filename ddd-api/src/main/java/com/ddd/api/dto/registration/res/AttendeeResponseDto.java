package com.ddd.api.dto.registration.res;

import java.time.Instant;

public record AttendeeResponseDto(
        Long userId,
        String status,
        Instant regDate,
        String email,
        String firstName,
        String lastName,
        String gender,
        String avatarUrl
//        String studentId,
//        int KNumber,
//        String majorName
) {
}
