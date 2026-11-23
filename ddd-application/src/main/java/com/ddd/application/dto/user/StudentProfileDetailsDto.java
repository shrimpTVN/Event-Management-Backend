package com.ddd.application.dto.user;

public record StudentProfileDetailsDto(
        String studentId,
        Integer kNumber,
        String schoolName,
        String majorName,
        String majorCode,
        String associationName
) {
}
