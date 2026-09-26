package com.backend.rootly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationLookupRequestDTO {

    @NotBlank(message = "text is required")
    @Size(max = 120, message = "text must not exceed 120 characters")
    private String text;

    @NotBlank(message = "sourceLanguage is required")
    @Pattern(regexp = "(?i)en|si", message = "sourceLanguage must be en or si")
    private String sourceLanguage;

    @NotBlank(message = "targetLanguage is required")
    @Pattern(regexp = "(?i)en|si", message = "targetLanguage must be en or si")
    private String targetLanguage;
}
