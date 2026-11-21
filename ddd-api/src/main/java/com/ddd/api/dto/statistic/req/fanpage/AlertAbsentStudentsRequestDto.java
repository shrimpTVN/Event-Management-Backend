package com.ddd.api.dto.statistic.req.fanpage;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AlertAbsentStudentsRequestDto(
        @NotNull Long semesterId,
        @NotEmpty List<Long> studentUserIds,
        String message
) {}
