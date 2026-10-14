package com.backend.rootly.dto.auth;

public record LoginResponseDTO(
        String id,
        String name,
        String email,
        String role,
        String verificationStatus,
        String token
) {
}
