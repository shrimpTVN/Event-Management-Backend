package com.ddd.application.dto.pointcategory;

import java.time.LocalDate;
import java.util.List;

public record PointCategoryGroupedTreeDto(
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
        List<PointCategoryFlattenedItemDto> items
) {
}
