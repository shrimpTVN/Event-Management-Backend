package com.ddd.api.dto.fanpage.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FanpageRegisterRequestDto(
        @NotNull @NotBlank String name,
        @NotNull @NotBlank String description,
        @NotNull @NotBlank String avatarUrl,
        @NotNull @NotBlank String orgType,
        @NotNull @NotBlank String orgName,
        @NotNull @NotBlank String parentOrg,
        String helpContact,
        String helpPhone
        ) {
}
