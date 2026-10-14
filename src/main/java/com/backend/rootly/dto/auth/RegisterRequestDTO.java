package com.backend.rootly.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegisterRequestDTO(

        @NotBlank(message = "Name field can't be empty")
        String name,

        @NotBlank(message = "Email field can't be empty")
        @Email(message = "Invalid Email format please provide correct email format")
        String email,

        @NotBlank(message = "Phone Number field can't be empty")
        @Size(min = 10, max = 10, message = "Phone number should have 10 digits")
        String phone,

        @NotBlank(message = "Gender field can't be empty")
        String gender,

        @NotBlank(message = "Password field can't be empty")
        @Size(min = 8, message = "Password must contain at least 8 characters")
        String password,

        String photoUrl,

        String district,

        @NotEmpty(message = "At least one language is required")
        List<String> languages

) {
}
