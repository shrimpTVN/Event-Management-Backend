package com.ddd.application.dto.statistic;

import java.time.Instant;

public record FanpageTopEventDto(
        Long eventId,
        String eventName,
        String bannerUrl,
        Instant dateHappen,
        int capacity,
        long registrationCount,
        long attendeeCount,
        double attendanceRate
) {}
