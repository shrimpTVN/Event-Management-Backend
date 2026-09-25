package com.ddd.application.dto.user;

public record UserSummaryDto(
        String email,
        String role,
        Boolean isActive
) {
}
