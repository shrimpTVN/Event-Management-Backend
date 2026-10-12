package com.ddd.api.dto.criteria.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriteriaRequestDto(
        @NotBlank(message = "Criteria name is required")
        @Size(max = 255, message = "Criteria name must not exceed 255 characters")
        String name,

        String note,

        String scope,

        @Min(value = 0, message = "Level must be greater than or equal to 0")
        Integer level,

        @Min(value = 0, message = "Children order must be greater than or equal to 0")
        Integer childrenOrder,

        Long parentId
) {
    // Deprecated alias for backwards-compatibility
    @Deprecated
    public String description() {
        return note;
    }
}

