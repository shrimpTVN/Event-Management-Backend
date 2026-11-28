package com.ddd.api.dto.fanpage.req;

import java.util.List;

public record FanpageFilterRequestDto(
        String keyword,
        String status,
        List<String> statuses,
        String orgType,
        Boolean isActive,
        Integer page,
        Integer size,
        String sortBy,
        String sortDir
) {
    public FanpageFilterRequestDto {
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 10;
        }
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        if (sortDir == null || sortDir.isBlank()) {
            sortDir = "desc";
        }
    }

    public FanpageFilterRequestDto(
            String keyword,
            String status,
            String orgType,
            Boolean isActive,
            Integer page,
            Integer size,
            String sortBy,
            String sortDir
    ) {
        this(keyword, status, null, orgType, isActive, page, size, sortBy, sortDir);
    }
}
