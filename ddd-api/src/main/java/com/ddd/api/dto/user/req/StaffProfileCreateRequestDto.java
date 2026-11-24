package com.ddd.api.dto.user.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StaffProfileCreateRequestDto(
        @NotNull @NotBlank @Size(max = 100) String firstName,
        @NotNull @NotBlank @Size(max = 100) String lastName,
        @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Gender must be MALE, FEMALE, or OTHER") String gender,
        @NotNull @Valid StaffProfileDetailsCreateRequestDto staffProfile
) {
}
