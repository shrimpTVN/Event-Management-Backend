package com.ddd.api.dto.auth.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequestDto(
        @NotBlank(message = "Email can't be blank")
        @Email(message = "Email is invalid")
        String email,

        @NotBlank(message = "OTP can't be blank")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
        String otpCode
) {
}
