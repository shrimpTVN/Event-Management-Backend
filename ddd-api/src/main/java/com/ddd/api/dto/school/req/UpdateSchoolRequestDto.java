package com.ddd.api.dto.school.req;

import jakarta.validation.constraints.NotBlank;

public record UpdateSchoolRequestDto(@NotBlank String name, String description, boolean isActive) {}
