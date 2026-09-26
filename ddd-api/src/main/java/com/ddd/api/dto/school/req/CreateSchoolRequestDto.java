package com.ddd.api.dto.school.req;
import jakarta.validation.constraints.NotBlank;

public record CreateSchoolRequestDto(@NotBlank String name, String description) {}

