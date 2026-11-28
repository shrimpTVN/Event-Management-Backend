package com.ddd.api.dto.user.res;

public record StudentProfileDetailsResponseDto(
        String studentId,
        Integer kNumber,
        String schoolName,
        String majorName,
        String majorCode,
        String associationName,
        Long schoolId,
        Long majorId
) {
    public StudentProfileDetailsResponseDto(
            String studentId,
            Integer kNumber,
            String schoolName,
            String majorName,
            String majorCode,
            String associationName
    ) {
        this(studentId, kNumber, schoolName, majorName, majorCode, associationName, null, null);
    }
}
