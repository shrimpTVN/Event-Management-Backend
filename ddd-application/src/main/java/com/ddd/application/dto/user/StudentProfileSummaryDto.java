package com.ddd.application.dto.user;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record StudentProfileSummaryDto(
        @NotNull String email,
        Set<String> roles,
        @NotNull String role,
        @NotNull Boolean isActive,
        @NotNull String firstName,
        @NotNull String lastName,
        @NotNull String studentId,
        @NotNull String gender,
        @NotNull Integer kNumber,
        @NotNull String avatarUrl,
        @NotNull String associationName,
        @NotNull String majorName,
        @NotNull String majorCode,
        @NotNull String schoolName
) {
    public StudentProfileSummaryDto(
            String email,
            String role,
            Boolean isActive,
            String firstName,
            String lastName,
            String studentId,
            String gender,
            Integer kNumber,
            String avatarUrl,
            String associationName,
            String majorName,
            String majorCode,
            String schoolName
    ) {
        this(email, role != null ? Set.of(role) : Set.of(), role, isActive, firstName, lastName, studentId, gender, kNumber, avatarUrl, associationName, majorName, majorCode, schoolName);
    }
}
