package com.ddd.api.dto.statistic.res.event;

public record EventOverviewStatisticResponseDto(
        int capacity,
        int maleQuantity,
        int femaleQuantity,
        long totalRegistrations,
        double capacityUtilizationRate,
        long totalAttended,
        double attendanceRate
) {
}
