package com.ddd.application.dto.criteria;

public record LeafProgressDetailDto(
        Long criteriaId,
        String eventName,
        String note,
        String completionSource
) {
}
