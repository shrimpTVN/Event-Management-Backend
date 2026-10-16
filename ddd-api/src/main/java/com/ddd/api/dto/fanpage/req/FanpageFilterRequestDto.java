package com.ddd.api.dto.fanpage.req;

public record FanpageFilterRequestDto(
        String keyword,
        String status,
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
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
        if (isActive == null) {
                isActive = true;
        }
    }
}
