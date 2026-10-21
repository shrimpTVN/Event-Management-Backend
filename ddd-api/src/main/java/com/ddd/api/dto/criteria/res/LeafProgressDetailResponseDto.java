package com.ddd.api.dto.criteria.res;

public record LeafProgressDetailResponseDto(
        Long criteriaId,
        String eventName,
        String note,
        String completionSource
) {
}
