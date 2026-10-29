package com.ddd.application.dto.event;

public record StoredEventStatusDto(
        Long eventId,
        boolean isStored
) {
}
