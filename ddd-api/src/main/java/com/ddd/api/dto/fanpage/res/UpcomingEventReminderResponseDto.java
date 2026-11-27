package com.ddd.api.dto.fanpage.res;

import java.time.Instant;

public record UpcomingEventReminderResponseDto(
        Long id,
        String title,
        Instant dateHappen,
        long registrationsCount
) {
}
