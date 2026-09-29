package com.ddd.api.dto.criteria.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriteriaRequestDto(
        @NotBlank @NotNull String name,
        @NotBlank @NotNull String description,
        @NotBlank @NotNull String scope
) {
}
