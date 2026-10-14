package com.backend.rootly.service.impl;

import com.backend.rootly.domain.TranslationLookup;
import com.backend.rootly.dto.ResponseDTO;
import com.backend.rootly.dto.response.TranslationGlossaryResponseDTO;
import com.backend.rootly.dto.response.TranslationLookupResponseDTO;
import com.backend.rootly.entity.TranslationEntry;
import com.backend.rootly.entity.TranslationMeaning;
import com.backend.rootly.enums.TranslationCategory;
import com.backend.rootly.exception.ResourceNotFoundException;
import com.backend.rootly.repository.TranslationEntryRepository;
import com.backend.rootly.service.TranslationService;
import com.backend.rootly.utility.ResponseGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TranslationServiceImplTests {

    private final TranslationEntryRepository repository = mock(TranslationEntryRepository.class);
    private TranslationService service;
    private TranslationEntry stupa;

    @BeforeEach
    void setUp() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("val.translation.lookup.success", Locale.ENGLISH,
                "Translation retrieved successfully");
        messages.addMessage("val.translation.glossary.success", Locale.ENGLISH,
                "Translation glossary retrieved successfully");
        service = new TranslationServiceImpl(repository, new ResponseGenerator(new ModelMapper(), messages));
        stupa = TranslationEntry.builder()
                .id("translation-1")
                .english("Stupa")
                .englishNormalized("stupa")
                .sinhala("ස්තූපය")
                .sinhalaNormalized("ස්තූපය")
                .transliteration("Stūpaya")
                .transliterationNormalized("stupaya")
                .pronunciation("stoo-pa-ya")
                .pronunciationNormalized("stoo-pa-ya")
                .meanings(List.of(new TranslationMeaning("noun", "A Buddhist monument.")))
                .exampleSentence("We visited the ancient stupa.")
                .relatedWords(List.of("temple", "relic"))
                .aliases(List.of("dagoba"))
                .aliasesNormalized(List.of("dagoba"))
                .category(TranslationCategory.TEMPLE)
                .active(true)
                .build();
    }

    @Test
    void looksUpEnglishAliasesAndReturnsTheCompleteEntry() {
        when(repository.findActiveEnglishLookup("dagoba")).thenReturn(Optional.of(stupa));

        ResponseDTO envelope = (ResponseDTO) service.lookup(
                new TranslationLookup("  DÁGOBA  ", "EN", "si"), Locale.ENGLISH).getBody();
        TranslationLookupResponseDTO result = (TranslationLookupResponseDTO) envelope.getData();

        assertThat(result.getTranslatedText()).isEqualTo("ස්තූපය");
        assertThat(result.getEnglish()).isEqualTo("Stupa");
        assertThat(result.getCategory()).isEqualTo("Temple");
        assertThat(result.getSource()).isEqualTo("WORD_BANK");
        assertThat(result.isEnriched()).isTrue();
        assertThat(result.getMeanings()).hasSize(1);
        verify(repository).findActiveEnglishLookup("dagoba");
    }

    @Test
    void looksUpSinhalaAndReturnsEnglish() {
        when(repository.findActiveSinhalaLookup("ස්තූපය")).thenReturn(Optional.of(stupa));

        ResponseDTO envelope = (ResponseDTO) service.lookup(
                new TranslationLookup("ස්තූපය", "si", "en"), Locale.ENGLISH).getBody();
        TranslationLookupResponseDTO result = (TranslationLookupResponseDTO) envelope.getData();

        assertThat(result.getTranslatedText()).isEqualTo("Stupa");
        assertThat(result.getSourceLanguage()).isEqualTo("si");
        assertThat(result.getTargetLanguage()).isEqualTo("en");
        verify(repository).findActiveSinhalaLookup("ස්තූපය");
    }

    @Test
    void reportsUnknownWordsWithoutInventingTranslations() {
        when(repository.findActiveEnglishLookup("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.lookup(
                new TranslationLookup("unknown", "en", "si"), Locale.ENGLISH))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Translation was not found in the offline word bank");
    }

    @Test
    void validatesLanguageDirectionAndPagingBeforeUsingMongo() {
        assertThatThrownBy(() -> service.lookup(
                new TranslationLookup("stupa", "en", "en"), Locale.ENGLISH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.lookup(
                new TranslationLookup("stupa", "fr", "si"), Locale.ENGLISH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.glossary(null, -1, 10, Locale.ENGLISH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.glossary(null, 0, 51, Locale.ENGLISH))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void filtersAndPaginatesTheGlossary() {
        PageRequest pageRequest = PageRequest.of(0, 4, Sort.by("englishNormalized").ascending());
        when(repository.findAllByActiveTrueAndCategory(TranslationCategory.TEMPLE, pageRequest))
                .thenReturn(new PageImpl<>(List.of(stupa), pageRequest, 18));

        ResponseDTO envelope = (ResponseDTO) service.glossary(
                "Temple", 0, 4, Locale.ENGLISH).getBody();
        TranslationGlossaryResponseDTO result = (TranslationGlossaryResponseDTO) envelope.getData();

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getEnglish()).isEqualTo("Stupa");
        assertThat(result.getTotal()).isEqualTo(18);
        assertThat(result.isHasNext()).isTrue();
        assertThat(envelope.getFullCount()).isEqualTo(18);
        verify(repository).findAllByActiveTrueAndCategory(TranslationCategory.TEMPLE, pageRequest);
    }
}
