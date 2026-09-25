package com.backend.rootly.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationLookup {
    private String text;
    private String sourceLanguage;
    private String targetLanguage;
}
