package com.ddd.api.dto.statistic.res.event;

public record DemoGraphicItemResponseDto(
        String label,
        long count,
        double percentage
) {
}
