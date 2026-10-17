package com.ddd.application.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class ServiceUtil {

    private ServiceUtil() {}

    public static Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        String sortField = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, sortField));
    }
}
