package com.ddd.application.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record StudentProfileDto(
                                @NotBlank String studentId,
                                @Positive @Max(100) Integer kNumber,
                                @Positive Integer majorId,
                                @Positive Integer userId) {
}
