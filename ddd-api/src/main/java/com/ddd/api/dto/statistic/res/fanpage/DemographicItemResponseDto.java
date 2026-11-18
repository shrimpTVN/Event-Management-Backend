package com.ddd.api.dto.statistic.res.fanpage;

public record DemographicItemResponseDto(
        String label,
        long count,
        double percentage
) {}
