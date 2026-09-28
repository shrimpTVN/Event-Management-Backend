package com.ddd.application.dto.fanpage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FanpageRegisterDto(
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
