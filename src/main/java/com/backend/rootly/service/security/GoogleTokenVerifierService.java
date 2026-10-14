package com.backend.rootly.service.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Verifies Google ID tokens issued to the Flutter client.
 * The token must have been issued for our registered Web Client ID.
 */
@Service
@Log4j2
public class GoogleTokenVerifierService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierService(@Value("${google.client.id}") String googleClientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    /**
     * Verifies the provided Google ID token string.
     *
     * @param idTokenString the raw ID token from the Flutter client
     * @return the verified {@link Payload} containing user info, or {@code null} if invalid
     */
    public Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken.getPayload();
            }
            if (log.isWarnEnabled()) {
                log.warn("Google ID token verification returned null — token may be invalid or expired");
            }
            return null;
        } catch (GeneralSecurityException | IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Error verifying Google ID token: {}", e.getMessage());
            }
            return null;
        }
    }
}
