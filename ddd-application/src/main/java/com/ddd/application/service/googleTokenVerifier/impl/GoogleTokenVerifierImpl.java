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

    @Value("${google.client-id}")
    private String clientId;

    @Override
    public GoogleUserInfo verifyToken(String idTokenString) {
        try {
            HttpTransport transport = new NetHttpTransport();
            JsonFactory jsonFactory = new GsonFactory();

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                log.warn("Google ID Token verification failed: token is invalid or expired");
                throw new BadCredentialsException("Invalid Google ID Token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

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
