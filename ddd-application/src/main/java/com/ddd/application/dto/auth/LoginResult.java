package com.ddd.application.dto.auth;

/**
 * Wrapper that separates user info (LoginDto) from the JWT token.
 * The token should only be placed in an HttpOnly cookie, never in the response body.
 */
public record LoginResult(LoginDto loginDto, String jwtToken) {}
