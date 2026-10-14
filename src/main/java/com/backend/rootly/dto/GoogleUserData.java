package com.backend.rootly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encapsulates verified user data extracted from a Google ID token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserData {
    private String email;
    private String name;
    private String pictureUrl;
}
