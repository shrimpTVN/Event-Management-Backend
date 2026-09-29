package com.ddd.api.dto.pointcategory.res;

import java.time.LocalDate;

public record PointCategoryResponseDto(
    Long id,
    String name,
    String note,
    Integer maximum,
    LocalDate dateApply,
    Integer level,
    Long parentCategoryId,
    boolean isActive,
    Integer childrenOrder
) {
}
