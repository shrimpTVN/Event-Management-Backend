package com.ddd.api.dto.major.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMajorRequestDto(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull Long schoolId
) {}