package com.ddd.application.dto.user;

public record UserSummaryDto(
        Long id,
        String email,
        String role,
        Boolean isActive
) {
}
