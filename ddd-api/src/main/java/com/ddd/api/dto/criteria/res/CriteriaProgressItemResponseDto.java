package com.ddd.api.dto.criteria.res;

import java.util.List;

public record CriteriaProgressItemResponseDto(
        Long id,
        String name,
        String note,
        String scope,
        Integer level,
        Integer childrenOrder,
        Integer requiredChildren,
        int completedChildrenCount,
        boolean isCompleted,
        List<CriteriaProgressItemResponseDto> children
) {
}
