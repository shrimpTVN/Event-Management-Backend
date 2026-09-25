package com.ddd.api.dto.role.req;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record RoleRequestDto(
        @NotBlank String name,
        String description,
        boolean isActive
) implements Serializable {}
