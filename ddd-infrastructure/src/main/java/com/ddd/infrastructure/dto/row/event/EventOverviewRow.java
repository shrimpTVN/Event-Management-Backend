package com.ddd.infrastructure.dto.row.event;

public record EventOverviewRow(
        int capacity,
        int maleQuantity,
        int femaleQuantity,
        long totalRegistrations,
        long totalAttended
) {
}
