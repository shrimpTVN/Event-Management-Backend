package com.ddd.application.dto.semesterpoint;


public record PointTransactionRequestInfoDto(
        Long semesterId,
        Long pointCategoryId,
        Long eventId,
        Integer points,
        String description
) {
}
