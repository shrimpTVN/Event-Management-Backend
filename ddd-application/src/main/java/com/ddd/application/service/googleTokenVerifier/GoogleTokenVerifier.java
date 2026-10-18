package com.ddd.application.service.googleTokenVerifier;

import com.ddd.application.dto.auth.GoogleUserInfo;

/**
 * Infrastructure port for verifying Google ID Tokens.
 * Lives in infrastructure because it communicates with Google's external auth server.
 */
public interface GoogleTokenVerifier {

    GoogleUserInfo verifyToken(String idToken);

}
