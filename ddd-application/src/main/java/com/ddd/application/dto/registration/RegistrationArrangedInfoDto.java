package com.ddd.application.dto.registration;

import com.ddd.domain.enums.RegistrationStatusEnum;

import java.time.Instant;

public record RegistrationArrangedInfoDto(
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
