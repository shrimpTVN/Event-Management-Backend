package com.ddd.api.dto.statistic.res.fanpage;

import java.time.Instant;

public record FanpageTopEventResponseDto(
        Long eventId,
        String eventName,
        String bannerUrl,
        Instant dateHappen,
        int capacity,
        long registrationCount,
        long attendeeCount,
        double attendanceRate
) {}
