package com.ddd.application.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record LoginDto(
        @NotNull Long userId,
        @NotBlank String email,
        Set<String> roles,
        @NotNull String role
) {
    public LoginDto(Long userId, String email, Set<String> roles) {
        this(userId, email, roles, (roles != null && !roles.isEmpty()) ? roles.iterator().next() : "");
    }

    public LoginDto(Long userId, String email, String role) {
        this(userId, email, role != null ? Set.of(role) : Set.of(), role);
    }
}
