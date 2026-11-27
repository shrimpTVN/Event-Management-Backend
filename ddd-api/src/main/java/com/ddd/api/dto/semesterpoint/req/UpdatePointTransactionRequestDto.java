package com.ddd.api.dto.semesterpoint.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdatePointTransactionRequestDto(@NotNull @Positive Integer points, String description) {}
