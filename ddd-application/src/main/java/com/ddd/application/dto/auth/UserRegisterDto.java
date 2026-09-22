package com.ddd.application.dto.auth;

public record UserRegisterDto(
        String firstName,
        String lastName,
        String email,
        String password
) {}
