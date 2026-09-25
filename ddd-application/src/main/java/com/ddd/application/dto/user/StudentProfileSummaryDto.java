package com.ddd.application.dto.user;

import jakarta.validation.constraints.NotNull;

public record StudentProfileSummaryDto(
        @NotNull String email,
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
}
