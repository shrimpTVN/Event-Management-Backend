package com.ddd.application.dto.criteria;

import java.util.List;

public record CriteriaProgressItemDto(
        Long id,
        String name,
        String note,
        String scope,
        Integer level,
        Integer childrenOrder,
        Integer requiredChildren,
        int completedChildrenCount,
        boolean isCompleted,
        List<CriteriaProgressItemDto> children
) {
}
