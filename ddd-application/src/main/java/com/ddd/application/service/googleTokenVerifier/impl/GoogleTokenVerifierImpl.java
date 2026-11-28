package com.ddd.application.service.googleTokenVerifier.impl;

import com.ddd.application.dto.auth.GoogleUserInfo;
import com.ddd.application.service.googleTokenVerifier.GoogleTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new GsonFactory();

    @Value("${google.client-id}")
    private String clientId;

    private volatile GoogleIdTokenVerifier cachedVerifier;
    private volatile String cachedClientId;

    private GoogleIdTokenVerifier getVerifier() {
        if (cachedVerifier == null || !Collections.singletonList(clientId).toString().equals(cachedClientId)) {
            synchronized (this) {
                String key = Collections.singletonList(clientId).toString();
                if (cachedVerifier == null || !key.equals(cachedClientId)) {
                    cachedVerifier = new GoogleIdTokenVerifier.Builder(HTTP_TRANSPORT, JSON_FACTORY)
                            .setAudience(Collections.singletonList(clientId))
                            .build();
                    cachedClientId = key;
                }
            }
        }
        return cachedVerifier;
    }

    @Override
    public GoogleUserInfo verifyToken(String idTokenString) {
        try {
            GoogleIdToken idToken = getVerifier().verify(idTokenString);
            if (idToken == null) {
                log.warn("Google ID Token verification failed: token is invalid or expired");
                throw new BadCredentialsException("Invalid Google ID Token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                log.warn("Google ID Token email is not verified for {}", payload.getEmail());
                throw new BadCredentialsException("Google email is not verified");
            }

            String givenName  = (String) payload.get("given_name");
            String familyName = (String) payload.get("family_name");
            String pictureUrl = (String) payload.get("picture");

            return GoogleUserInfo.builder()
                    .email(payload.getEmail())
                    .googleId(payload.getSubject())
                    .firstName(givenName  != null ? givenName  : "")
                    .lastName(familyName != null ? familyName : "")
                    .pictureUrl(pictureUrl)
                    .build();

        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error verifying Google ID Token: {}", e.getMessage(), e);
            throw new BadCredentialsException("Failed to verify Google ID Token: " + e.getMessage());
        }
    }
}
