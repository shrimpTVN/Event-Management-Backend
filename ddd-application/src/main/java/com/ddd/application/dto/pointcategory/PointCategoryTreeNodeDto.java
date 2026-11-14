package com.ddd.application.dto.pointcategory;

import java.time.LocalDate;
import java.util.List;

public record PointCategoryTreeNodeDto(
        Long id,
        String name,
        String note,
        Integer maximum,
        LocalDate dateApply,
        Integer level,
        Long parentCategoryId,
        boolean isActive,
        Integer childrenOrder,
        Boolean isMaxChild,
        List<PointCategoryTreeNodeDto> children
) {
}
