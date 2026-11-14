package com.ddd.api.dto.pointcategory.res;

import java.time.LocalDate;
import java.util.List;

public record PointCategoryTreeNodeResponseDto(
        Long id,
        String name,
        String note,
        Integer maximum,
        LocalDate dateApply,
        Integer level,
        Long parentCategoryId,
        boolean isActive,
        Integer childrenOrder,
        boolean isMaxChild,
        List<PointCategoryTreeNodeResponseDto> children
) {
}
