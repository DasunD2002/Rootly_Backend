package com.backend.rootly.controller;

import com.backend.rootly.config.MapperConfiguration;
import com.backend.rootly.domain.TranslationLookup;
import com.backend.rootly.service.TranslationService;
import com.backend.rootly.utility.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TranslationControllerTests {

    private final TranslationService service = mock(TranslationService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ModelMapper modelMapper = new MapperConfiguration().modelMapper();
        mvc = MockMvcBuilders.standaloneSetup(new TranslationController(service, modelMapper)).build();
    }

    @Test
    void mapsAValidLookupRequestToTheService() throws Exception {
        when(service.lookup(any(TranslationLookup.class), eq(Locale.ENGLISH)))
                .thenReturn(ResponseEntity.ok(Map.of("responseCode", ResponseCode.TRANSLATION_LOOKUP_SUCCESS)));

        mvc.perform(post("/api/v1/translations/lookup")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"stupa","sourceLanguage":"en","targetLanguage":"si"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value(ResponseCode.TRANSLATION_LOOKUP_SUCCESS));

        ArgumentCaptor<TranslationLookup> request = ArgumentCaptor.forClass(TranslationLookup.class);
        verify(service).lookup(request.capture(), eq(Locale.ENGLISH));
        assertThat(request.getValue()).isEqualTo(new TranslationLookup("stupa", "en", "si"));
    }

    @Test
    void rejectsMissingTextAndUnsupportedLanguagesBeforeCallingTheService() throws Exception {
        for (String body : new String[]{
                "{\"text\":\"\",\"sourceLanguage\":\"en\",\"targetLanguage\":\"si\"}",
                "{\"text\":\"stupa\",\"sourceLanguage\":\"fr\",\"targetLanguage\":\"si\"}",
                "{\"text\":\"stupa\",\"sourceLanguage\":\"en\"}"
        }) {
            mvc.perform(post("/api/v1/translations/lookup")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void passesGlossaryFiltersAndPaginationToTheService() throws Exception {
        when(service.glossary("Temple", 1, 4, Locale.ENGLISH))
                .thenReturn(ResponseEntity.ok(Map.of("responseCode", ResponseCode.TRANSLATION_GLOSSARY_SUCCESS)));

        mvc.perform(get("/api/v1/translations/glossary")
                        .header("Accept-Language", "en")
                        .param("category", "Temple").param("page", "1").param("size", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value(ResponseCode.TRANSLATION_GLOSSARY_SUCCESS));

        verify(service).glossary("Temple", 1, 4, Locale.ENGLISH);
    }
}
