package com.ddd.api.dto.user.res;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record StudentProfileResponseDto(
        @NotNull String email,
        Set<String> roles,
        @NotNull String role,
        @NotNull Boolean isActive,
        @NotNull String firstName,
        @NotNull String lastName,
        @NotNull String studentId,
        @NotNull String gender,
        @NotNull String kNumber,
        @NotNull String avatarUrl,
        @NotNull String associationName,
        @NotNull String majorName,
        @NotNull String majorCode,
        @NotNull String schoolName
) {
    public StudentProfileResponseDto(
            String email,
            String role,
            Boolean isActive,
            String firstName,
            String lastName,
            String studentId,
            String gender,
            String kNumber,
            String avatarUrl,
            String associationName,
            String majorName,
            String majorCode,
            String schoolName
    ) {
        this(email, role != null ? Set.of(role) : Set.of(), role, isActive, firstName, lastName, studentId, gender, kNumber, avatarUrl, associationName, majorName, majorCode, schoolName);
    }
}
