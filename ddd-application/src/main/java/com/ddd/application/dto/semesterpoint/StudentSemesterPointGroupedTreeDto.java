package com.ddd.application.dto.semesterpoint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StudentSemesterPointGroupedTreeDto(
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
        Long studentSemesterPointId,
        Integer totalPoint,
        Integer systemPointRecord,
        Integer manualPointRecord,
        Instant finalizedAt,
        List<StudentSemesterPointFlattenedItemDto> items
) {
}
