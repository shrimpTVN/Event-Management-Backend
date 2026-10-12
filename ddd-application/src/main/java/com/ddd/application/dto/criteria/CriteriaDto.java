package com.ddd.application.dto.criteria;

public record CriteriaDto(
        Long id,
        String name,
        String note,
        String scope,
        Integer level,
        Integer childrenOrder,
        Long parentId,
        boolean isActive
) {
    // Deprecated accessor for backward compatibility
    @Deprecated
    public String description() {
        return note;
    }
}
