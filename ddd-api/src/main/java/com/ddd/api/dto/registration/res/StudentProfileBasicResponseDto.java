package com.ddd.api.dto.registration.res;

public record StudentProfileBasicResponseDto(
        String email,
        String firstName,
        String lastName,
        String studentId,
        String gender,
        Integer kNumber,
        String avatarUrl,
        String majorName,
        String majorCode
) {
}
