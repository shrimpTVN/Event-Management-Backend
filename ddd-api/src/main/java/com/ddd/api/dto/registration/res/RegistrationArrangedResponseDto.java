package com.ddd.api.dto.registration.res;

import com.ddd.domain.enums.RegistrationStatusEnum;

import java.time.Instant;

public record RegistrationArrangedResponseDto(
        Long eventId,
        String eventName,
        String eventDescription,
        String eventLocation,
        String eventBannerUrl,
        Instant eventDateHappen,
        Instant regDate,
        RegistrationStatusEnum status
) {
}
