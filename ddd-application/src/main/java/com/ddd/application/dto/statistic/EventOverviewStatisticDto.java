package com.ddd.application.dto.statistic;

public record EventOverviewStatisticDto(
        int capacity,
        int maleQuantity,
        int femaleQuantity,
        long totalRegistrations,
        double capacityUtilizationRate,
        long totalAttended,
        double attendanceRate
) {
}
