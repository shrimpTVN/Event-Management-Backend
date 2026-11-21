package com.ddd.application.dto.registration;

public record StudentProfileBasicInfoDto(
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
