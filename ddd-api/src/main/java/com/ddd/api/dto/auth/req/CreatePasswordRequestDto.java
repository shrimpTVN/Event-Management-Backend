package com.ddd.api.dto.auth.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePasswordRequestDto(
        @NotBlank(message = "New password must not be blank")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String newPassword
) {
}
