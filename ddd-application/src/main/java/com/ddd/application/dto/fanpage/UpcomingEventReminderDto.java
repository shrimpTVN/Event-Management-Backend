package com.ddd.application.dto.fanpage;

import java.time.Instant;

public record UpcomingEventReminderDto(
        Long id,
        String title,
        Instant dateHappen,
        long registrationsCount
) {
}
