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
public class TranslationEntryResponseDTO {
    private String id;
    private String english;
    private String sinhala;
    private String transliteration;
    private String pronunciation;
    private List<TranslationMeaningDTO> meanings;
    private String exampleSentence;
    private List<String> relatedWords;
    private List<String> aliases;
    private String category;
}
