package com.ddd.application.dto.criteria;

import java.time.Instant;
import java.util.List;

public record CriteriaProgressNodeDto(
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
        List<CriteriaProgressNodeDto> children
) {
}
