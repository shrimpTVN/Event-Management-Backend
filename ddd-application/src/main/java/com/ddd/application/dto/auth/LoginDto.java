package com.ddd.application.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseCookie;

public record LoginDto(@NotNull Long userId,
                       @NotBlank String name,
                       @NotBlank String username,
                       @NotBlank String email,
                       @NotNull String role,
                       @NotNull String jwtToken) {}
