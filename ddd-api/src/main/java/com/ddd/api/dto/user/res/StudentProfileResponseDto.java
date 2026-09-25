package com.ddd.api.dto.user.res;

import jakarta.validation.constraints.NotNull;

public record StudentProfileResponseDto(
        @NotNull String email,
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
}
