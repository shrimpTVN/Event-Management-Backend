package com.ddd.application.dto.fanpage;

public record FanpageSearchCriteriaDto(
        String keyword,
        String status,
        String orgType,
        Boolean isActive,
        int page,
        int size,
        String sortBy,
        String sortDir
) {
}
