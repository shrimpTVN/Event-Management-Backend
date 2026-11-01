package com.ddd.application.dto.criteria;

import java.util.List;

public record CriteriaTreeNodeDto(
        Long id,
        String name,
        String note,
        String scope,
        Integer level,
        Integer childrenOrder,
        Integer requiredChildren,
        List<CriteriaTreeNodeDto> children
) {
}
