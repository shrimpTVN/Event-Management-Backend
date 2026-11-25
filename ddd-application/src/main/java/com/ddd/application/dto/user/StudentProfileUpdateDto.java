package com.ddd.application.dto.user;

public record StudentProfileUpdateDto(
        String firstName,
        String lastName,
        String gender,
        Integer kNumber,
        String avatarUrl,
        Integer associationId,
        Integer schoolId,
        Integer majorId
) {
}
