package com.ddd.api.dto.event.res;

public record StoredEventStatusResponseDto(
        Long eventId,
        boolean isStored
) {
}
