package com.ddd.api.dto.user.res;

public record StudentProfileDetailsResponseDto(
        String studentId,
        Integer kNumber,
        String schoolName,
        String majorName,
        String majorCode,
        String associationName
) {
}
