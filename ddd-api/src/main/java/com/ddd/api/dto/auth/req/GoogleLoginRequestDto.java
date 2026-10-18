package com.ddd.api.dto.auth.req;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequestDto(
        @NotBlank(message = "idToken can not Blank")
        String idToken

) {
}
