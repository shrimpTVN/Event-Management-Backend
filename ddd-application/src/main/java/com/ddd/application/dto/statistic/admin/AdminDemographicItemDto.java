package com.ddd.application.dto.statistic.admin;

public record AdminDemographicItemDto(
        String label,
        long count,
        double percentage
) {}
