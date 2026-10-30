package com.ddd.api.dto.auth.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendOtpRequestDto(
        @NotBlank(message = "Email can't be blank")
        @Email(message = "Email is invalid")
        String email
) {
}
