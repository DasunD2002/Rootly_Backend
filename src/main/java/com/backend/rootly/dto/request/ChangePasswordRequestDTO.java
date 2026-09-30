package com.backend.rootly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequestDTO {
    
    @NotBlank(message = "Current password is required")
    private String currentPassword;
    
    @NotBlank(message = "New password field can't be empty")
    @Size(min = 8, message = "New password must contain at least 8 characters")
    private String newPassword;
}
