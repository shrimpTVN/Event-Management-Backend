package com.ddd.api.dto.criteria.req;

import jakarta.validation.constraints.NotNull;

public record ManualCriteriaProgressRequestDto(
        @NotNull Long academicYearId,
        @NotNull Long criteriaId,
        @NotNull String note
) {
}
