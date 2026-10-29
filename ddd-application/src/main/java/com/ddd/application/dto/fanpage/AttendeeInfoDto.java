package com.ddd.application.dto.fanpage;

import java.time.Instant;

public record AttendeeInfoDto(
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
