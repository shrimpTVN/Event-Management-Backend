package com.ddd.application.dto.pointcategory;

import java.time.LocalDate;

public record PointCategoryInfoDto(
        Long id,
        String name,
        String note,
        Integer maximum,
        LocalDate dateApply,
        Integer level,
        Long parentCategoryId,
        boolean isActive
) {
}
