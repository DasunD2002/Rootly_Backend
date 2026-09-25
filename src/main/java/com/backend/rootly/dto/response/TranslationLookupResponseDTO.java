package com.backend.rootly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationLookupResponseDTO {
    private String id;
    private String sourceText;
    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
    private String english;
    private String sinhala;
    private String transliteration;
    private String pronunciation;
    private List<TranslationMeaningDTO> meanings;
    private String exampleSentence;
    private List<String> relatedWords;
    private String category;
    private String source;
    private boolean enriched;
}
