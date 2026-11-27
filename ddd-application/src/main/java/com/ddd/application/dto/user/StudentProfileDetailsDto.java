package com.ddd.application.dto.user;

public record StudentProfileDetailsDto(
        String studentId,
        Integer kNumber,
        String schoolName,
        String majorName,
        String majorCode,
        String associationName,
        Long schoolId,
        Long majorId
) {
    public StudentProfileDetailsDto(
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
