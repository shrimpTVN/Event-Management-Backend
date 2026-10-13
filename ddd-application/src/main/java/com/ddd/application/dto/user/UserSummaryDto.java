package com.ddd.application.dto.user;

import java.util.Set;

public record UserSummaryDto(
        Long userId,
        String email,
        Set<String> roles,
        String role,
        Boolean isActive
) {
    public UserSummaryDto(Long userId, String email, Set<String> roles, Boolean isActive) {
        this(userId, email, roles, (roles != null && !roles.isEmpty()) ? roles.iterator().next() : null, isActive);
    }

    public UserSummaryDto(String email, Set<String> roles, Boolean isActive) {
        this(null, email, roles, isActive);
    }
}
