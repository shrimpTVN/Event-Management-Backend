package com.ddd.api.dto.criteria.req;

import jakarta.validation.constraints.*;

public record CriteriaRequestDto(
        @NotBlank(message = "Criteria name is required")
        @NotNull(message = "Criteria name is required")
        @Size(max = 255, message = "Criteria name must not exceed 255 characters")
        String name,
        String note,
        @NotNull(message = "Criteria scope is required")
        String scope,

        @NotNull
        @Positive(message = "Criteria level must be a positive integer")
        Integer childrenOrder,
        Long parentId
) {
    // Deprecated alias for backwards-compatibility
    @Deprecated
    public String description() {
        return note;
    }
}

