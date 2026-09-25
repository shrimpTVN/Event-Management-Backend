package com.ddd.api.dto.role.res;

public record RoleResponseDto(
        Long id,
        String name,
        String description,
        boolean isActive
) {}
