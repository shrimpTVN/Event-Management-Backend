package com.ddd.application.dto.statistic;

import java.util.List;

public record AlertAbsentStudentsDto(
        Long semesterId,
        List<Long> studentUserIds,
        String message
) {}
