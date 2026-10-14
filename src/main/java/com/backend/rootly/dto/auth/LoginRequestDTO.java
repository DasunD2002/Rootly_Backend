package com.backend.rootly.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(

        @NotBlank(message = "This section cant be Blank")
        @Email(message = "Invalid Email type")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
