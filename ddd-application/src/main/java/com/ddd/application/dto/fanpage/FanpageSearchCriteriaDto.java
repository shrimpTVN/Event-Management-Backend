package com.ddd.application.dto.fanpage;

import java.util.List;

public record FanpageSearchCriteriaDto(
        String keyword,
        String status,
        List<String> statuses,
        String orgType,
        Boolean isActive,
        int page,
        int size,
        String sortBy,
        String sortDir
) {
    public FanpageSearchCriteriaDto(
            String keyword,
            String status,
            String orgType,
            Boolean isActive,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        this(keyword, status, null, orgType, isActive, page, size, sortBy, sortDir);
    }
}
