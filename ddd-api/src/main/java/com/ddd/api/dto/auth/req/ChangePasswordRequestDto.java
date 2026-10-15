package com.ddd.api.dto.auth.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDto(
        @NotNull @Size(min=6) String oldPassword,
        @NotNull @Size(min=6) String newPassword
) {
}
