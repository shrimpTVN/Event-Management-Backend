package com.ddd.api.dto.criteria.res;

import java.time.Instant;
import java.util.List;

public record CriteriaProgressNodeResponseDto(
        Long criteriaId,
        String name,
        String note,
        String scope,
        Integer level,
        Integer childrenOrder,
        Integer requiredChildren,
        boolean isCompleted,
        Integer completedChildren,
        Integer totalChildren,
        String completionSource,
        Instant completedAt,
        List<CriteriaProgressNodeResponseDto> children
) {
}
