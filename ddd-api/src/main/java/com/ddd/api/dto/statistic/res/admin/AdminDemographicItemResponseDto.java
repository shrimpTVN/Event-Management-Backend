package com.ddd.api.dto.statistic.res.admin;

public record AdminDemographicItemResponseDto(
        String label,
        long count,
        double percentage
) {}
