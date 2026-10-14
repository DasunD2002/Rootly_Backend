package com.backend.rootly.service.impl;

import com.backend.rootly.domain.TranslationLookup;
import com.backend.rootly.dto.response.TranslationEntryResponseDTO;
import com.backend.rootly.dto.response.TranslationGlossaryResponseDTO;
import com.backend.rootly.dto.response.TranslationLookupResponseDTO;
import com.backend.rootly.dto.response.TranslationMeaningDTO;
import com.backend.rootly.entity.TranslationEntry;
import com.backend.rootly.enums.TranslationCategory;
import com.backend.rootly.exception.ResourceNotFoundException;
import com.backend.rootly.repository.TranslationEntryRepository;
import com.backend.rootly.service.TranslationService;
import com.backend.rootly.utility.MessageConstant;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TranslationServiceImpl implements TranslationService {

    private static final String ENGLISH = "en";
    private static final String SINHALA = "si";
    private static final int MAX_TEXT_LENGTH = 120;
    private static final int MAX_PAGE_SIZE = 50;
    private final TranslationEntryRepository repository;
    private final ResponseGenerator responseGenerator;

    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public ResponseEntity<Object> lookup(TranslationLookup request, Locale locale) {
        if (request == null) {
            throw new IllegalArgumentException("Translation lookup request is required");
        }
        String sourceLanguage = requireLanguage(request.getSourceLanguage(), "sourceLanguage");
        String targetLanguage = requireLanguage(request.getTargetLanguage(), "targetLanguage");
        if (sourceLanguage.equals(targetLanguage)) {
            throw new IllegalArgumentException("sourceLanguage and targetLanguage must be different");
        }
        String sourceText = requireText(request.getText());
        String normalizedText = normalize(sourceText);
        TranslationEntry entry = ENGLISH.equals(sourceLanguage)
                ? repository.findActiveEnglishLookup(normalizedText)
                    .orElseThrow(TranslationServiceImpl::notFound)
                : repository.findActiveSinhalaLookup(normalizedText)
                    .orElseThrow(TranslationServiceImpl::notFound);

        TranslationLookupResponseDTO result = TranslationLookupResponseDTO.builder()
                .id(entry.getId())
                .sourceText(sourceText)
                .translatedText(ENGLISH.equals(targetLanguage) ? entry.getEnglish() : entry.getSinhala())
                .sourceLanguage(sourceLanguage)
                .targetLanguage(targetLanguage)
                .english(entry.getEnglish())
                .sinhala(entry.getSinhala())
                .transliteration(entry.getTransliteration())
                .pronunciation(entry.getPronunciation())
                .meanings(entry.getMeanings().stream().map(TranslationServiceImpl::toMeaning).toList())
                .exampleSentence(entry.getExampleSentence())
                .relatedWords(entry.getRelatedWords())
                .category(entry.getCategory().getLabel())
                .source("WORD_BANK")
                .enriched(true)
                .build();

        return responseGenerator.generateSuccessResponse(request, HttpStatus.OK,
                ResponseCode.TRANSLATION_LOOKUP_SUCCESS, MessageConstant.TRANSLATION_LOOKUP_SUCCESS,
                locale, result);
    }

    @Override
    public ResponseEntity<Object> glossary(String category, int page, int size, Locale locale) {
        validatePage(page, size);
        TranslationCategory selectedCategory = category == null || category.isBlank()
                ? null : TranslationCategory.fromValue(category);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("englishNormalized").ascending());
        Page<TranslationEntry> result = selectedCategory == null
                ? repository.findAllByActiveTrue(pageable)
                : repository.findAllByActiveTrueAndCategory(selectedCategory, pageable);
        TranslationGlossaryResponseDTO response = TranslationGlossaryResponseDTO.builder()
                .items(result.getContent().stream().map(TranslationServiceImpl::toEntry).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .total(result.getTotalElements())
                .hasNext(result.hasNext())
                .build();
        return responseGenerator.generateSuccessResponse(null, HttpStatus.OK,
                ResponseCode.TRANSLATION_GLOSSARY_SUCCESS, MessageConstant.TRANSLATION_GLOSSARY_SUCCESS,
                locale, response, result.getTotalElements());
    }

    public static String normalize(String value) {
        String compact = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        boolean containsSinhala = compact.codePoints().anyMatch(codePoint -> codePoint >= 0x0D80 && codePoint <= 0x0DFF);
        return containsSinhala ? compact
                : Normalizer.normalize(compact, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    @SuppressWarnings("PMD.LawOfDemeter")
    private static TranslationEntryResponseDTO toEntry(TranslationEntry entry) {
        return TranslationEntryResponseDTO.builder()
                .id(entry.getId())
                .english(entry.getEnglish())
                .sinhala(entry.getSinhala())
                .transliteration(entry.getTransliteration())
                .pronunciation(entry.getPronunciation())
                .meanings(entry.getMeanings().stream().map(TranslationServiceImpl::toMeaning).toList())
                .exampleSentence(entry.getExampleSentence())
                .relatedWords(entry.getRelatedWords())
                .aliases(entry.getAliases())
                .category(entry.getCategory().getLabel())
                .build();
    }

    private static TranslationMeaningDTO toMeaning(com.backend.rootly.entity.TranslationMeaning meaning) {
        return new TranslationMeaningDTO(meaning.getPartOfSpeech(), meaning.getText());
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Translation was not found in the offline word bank");
    }

    private static String requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        String trimmed = text.trim();
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("text must not exceed " + MAX_TEXT_LENGTH + " characters");
        }
        return trimmed;
    }

    private static String requireLanguage(String language, String fieldName) {
        String normalized = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
        if (!ENGLISH.equals(normalized) && !SINHALA.equals(normalized)) {
            throw new IllegalArgumentException(fieldName + " must be en or si");
        }
        return normalized;
    }

    private static void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and 50");
        }
    }
}
