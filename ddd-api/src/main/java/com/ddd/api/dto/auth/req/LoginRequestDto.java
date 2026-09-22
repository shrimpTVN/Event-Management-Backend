package com.ddd.api.dto.auth.req;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(@NotBlank String email, @NotBlank String password) {
}
