package com.ddd.api.dto.criteria.res;

public record CriteriaResponseDto(
        Long id,
        String name,
        String note,
        String scope,
        Integer level,
        Integer childrenOrder,
        Long parentId,
        boolean isActive
) {
    // Deprecated alias for backwards-compatibility
    @Deprecated
    public String description() {
        return note;
    }
}

