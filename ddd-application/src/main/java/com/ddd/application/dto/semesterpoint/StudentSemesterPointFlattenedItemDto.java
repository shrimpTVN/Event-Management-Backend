package com.ddd.application.dto.semesterpoint;

import java.time.Instant;
import java.time.LocalDate;

public record StudentSemesterPointFlattenedItemDto(
        Long id,
        String name,
        String note,
        Integer maximum,
        LocalDate dateApply,
        Integer level,
        Long parentCategoryId,
        boolean isActive,
        Integer childrenOrder,
        boolean isLeaf,
        Boolean isMaxChild,
        Long studentSemesterPointId,
        Integer totalPoint,
        Integer systemPointRecord,
        Integer manualPointRecord,
        Instant finalizedAt
) {
}
