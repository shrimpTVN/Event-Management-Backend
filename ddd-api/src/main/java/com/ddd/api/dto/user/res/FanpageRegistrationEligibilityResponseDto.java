package com.ddd.api.dto.user.res;

public record FanpageRegistrationEligibilityResponseDto(
        String status,
        String reasonCode,
        Boolean requiresReauthentication
) {
}
