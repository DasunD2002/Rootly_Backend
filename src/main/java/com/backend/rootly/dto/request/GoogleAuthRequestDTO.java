package com.backend.rootly.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleAuthRequestDTO {

    /**
     * The Google ID token obtained by the Flutter client after the user
     * signs in with Google. The backend verifies this token against
     * Google's public keys before issuing its own JWT.
     */
    @NotBlank(message = "Google ID token must not be blank")
    private String idToken;

    /**
     * Optional: preferred language(s) the user speaks.
     * Needed only during first-time (registration) flow.
     */
    private List<String> languages;
}
