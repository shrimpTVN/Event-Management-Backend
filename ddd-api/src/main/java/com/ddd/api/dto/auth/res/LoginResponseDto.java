package com.ddd.api.dto.auth.res;

public record LoginResponseDto(Long userId,
                               String name,
                               String username,
                               String email,
                               String role){}
