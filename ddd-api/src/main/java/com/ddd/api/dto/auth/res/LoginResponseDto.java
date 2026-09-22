package com.ddd.api.dto.auth.res;

public record LoginResponseDto(Long userId,
                               String email,
                               String role){}
