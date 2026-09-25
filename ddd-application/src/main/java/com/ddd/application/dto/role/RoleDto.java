package com.ddd.application.dto.role;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record RoleDto(
        Long id,
        @NotBlank String name,
        String description,
        boolean isActive
) implements Serializable {}
