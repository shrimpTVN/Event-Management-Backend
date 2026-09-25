package com.ddd.api.dto.user.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StudentProfileRequestDto(
        @NotNull @NotBlank String firstName,
        @NotNull @NotBlank String lastName,
        @NotNull @NotBlank String gender,
        @NotNull @Positive @Max(100) Integer kNumber,
        @NotNull @NotBlank String avatarUrl,
        @NotNull @Positive Integer associationId,
        @NotNull @Positive Integer majorId
) {
}
