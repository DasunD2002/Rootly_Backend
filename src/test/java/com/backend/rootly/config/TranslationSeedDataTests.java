package com.backend.rootly.config;

import com.backend.rootly.entity.TranslationEntry;
import com.backend.rootly.enums.TranslationCategory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationSeedDataTests {

    @Test
    void containsSixtyCompleteUniqueEntriesAcrossTheFourUiCategories() throws Exception {
        List<TranslationEntry> entries;
        try (InputStream input = new ClassPathResource("data/translations.json").getInputStream()) {
            entries = Arrays.asList(JsonMapper.builder().build()
                    .readValue(input, TranslationEntry[].class));
        }

        assertThat(entries).hasSize(60);
        assertThat(entries).extracting(TranslationEntry::getEnglish).doesNotHaveDuplicates();
        assertThat(entries).filteredOn(entry -> entry.getCategory() == TranslationCategory.TEMPLE).hasSize(18);
        assertThat(entries).filteredOn(entry -> entry.getCategory() == TranslationCategory.GREETINGS).hasSize(14);
        assertThat(entries).filteredOn(entry -> entry.getCategory() == TranslationCategory.FOOD).hasSize(14);
        assertThat(entries).filteredOn(entry -> entry.getCategory() == TranslationCategory.DIRECTIONS).hasSize(14);
        assertThat(entries).allSatisfy(entry -> {
            assertThat(entry.getEnglish()).isNotBlank();
            assertThat(entry.getSinhala()).isNotBlank();
            assertThat(entry.getTransliteration()).isNotBlank();
            assertThat(entry.getPronunciation()).isNotBlank();
            assertThat(entry.getMeanings()).isNotEmpty();
            assertThat(entry.getExampleSentence()).isNotBlank();
            assertThat(entry.getRelatedWords()).isNotEmpty();
            assertThat(entry.getAliases()).isNotNull();
        });
    }
}
