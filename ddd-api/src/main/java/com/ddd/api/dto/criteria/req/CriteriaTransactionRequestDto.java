package com.ddd.api.dto.criteria.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CriteriaTransactionRequestDto(
        @NotNull @Positive Long criteriaId,
        @NotNull @Positive Long academicYearId,
        @NotNull String note
) {
}
