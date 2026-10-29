package com.ddd.api.dto.auth.res;

import java.util.Set;

public record LoginResponseDto(
        Long userId,
        String email,
        Set<String> roles,
        String role
) {

}
