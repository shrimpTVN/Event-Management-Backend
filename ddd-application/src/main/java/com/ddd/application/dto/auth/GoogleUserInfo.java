package com.ddd.application.dto.auth;

import lombok.Builder;

/**
 * DTO holding the verified Google user information extracted from the ID Token.
 */
@Builder
public record GoogleUserInfo(
        String email,
        String googleId,   // Google's "sub" field - unique stable user identifier
        String firstName,
        String lastName,
        String pictureUrl
) {
}
