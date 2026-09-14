package com.ddd.application.dto.auth;

public record UserRegisterDto(
        String name,
        int age,
        String username,
        String email,
        String password
) {}
