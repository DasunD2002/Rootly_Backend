package com.backend.rootly.dto.auth;

public record RegisterResponseDTO(
        String id,
        String name,
        String email,
        String role,
        String verificationStatus
) {
}
