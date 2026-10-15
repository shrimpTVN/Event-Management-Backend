package com.ddd.application.dto.auth;

/**
 * Result returned by the refresh token flow containing new access and refresh tokens.
 */
public record TokenRefreshResultDto(
        String newAccessToken,
        String newRawRefreshToken
) {}
