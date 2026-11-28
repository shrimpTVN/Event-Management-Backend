package com.ddd.api.dto.criteria.req;

import jakarta.validation.constraints.NotNull;

public record CriteriaTransactionNoteRequestDto(@NotNull String note) {}
