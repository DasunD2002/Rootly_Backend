package com.backend.rootly.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequestDTO {

    @NotBlank(message = "Name field can't be empty")
    private String name;

    @NotBlank(message = "Email field can't be empty")
    @Email(message = "Invalid Email format please provide correct email format")
    private String email;

    @NotBlank(message = "Phone Number field can't be empty")
    @Size(min = 10, max = 10, message = "Phone number should have 10 digits")
    private String phone;

    @NotBlank(message = "Gender field can't be empty")
    private String gender;

    @NotBlank(message = "Password field can't be empty")
    @Size(min = 8, message = "Password must contain at least 8 characters")
    private String password;

    private String photoUrl;

    private String district;

    @NotEmpty(message = "At least one language is required")
    private List<String> languages;
}
