package com.ddd.application.dto.auth;

/**
 * Wrapper that separates user info (LoginDto) from the JWT access token and refresh token.
 * Tokens should only be placed in HttpOnly cookies, never in the response body.
 */
public record LoginResult(LoginDto loginDto, String jwtToken, String rawRefreshToken) {}
