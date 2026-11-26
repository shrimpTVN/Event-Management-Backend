package com.ddd.api.dto.criteria.res;

import com.ddd.application.dto.criteria.CriteriaTreeNodeDto;

import java.util.List;

public record CriteriaTreeNodeResponseDto(
        Long id,
        String name,
        String note,
        String scope,
        Integer level,
        Integer childrenOrder,
        Integer requiredChildren,
        Integer requiredOccurrences,
        List<CriteriaTreeNodeDto> children
) {
}
