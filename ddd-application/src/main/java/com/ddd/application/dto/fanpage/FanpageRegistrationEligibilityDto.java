package com.ddd.application.dto.fanpage;

public record FanpageRegistrationEligibilityDto(
        String status,
        String reasonCode,
        Boolean requiresReauthentication
) {
}
