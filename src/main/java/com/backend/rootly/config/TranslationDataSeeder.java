package com.backend.rootly.config;

import com.backend.rootly.entity.TranslationEntry;
import com.backend.rootly.repository.TranslationEntryRepository;
import com.backend.rootly.service.impl.TranslationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Log4j2
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rootly.translation.seed-enabled", havingValue = "true", matchIfMissing = true)
public class TranslationDataSeeder implements ApplicationRunner {

    private final TranslationEntryRepository repository;
    private final JsonMapper jsonMapper;

    @Value("${rootly.translation.seed-resource:classpath:data/translations.json}")
    private Resource seedResource;

    @Override
    public void run(ApplicationArguments arguments) {
        try (InputStream input = seedResource.getInputStream()) {
            List<TranslationEntry> seeds = Arrays.asList(jsonMapper.readValue(input, TranslationEntry[].class));
            Set<String> existing = new HashSet<>();
            repository.findAll().forEach(entry -> existing.add(entry.getEnglishNormalized()));
            List<TranslationEntry> missing = seeds.stream()
                    .map(TranslationDataSeeder::prepare)
                    .filter(entry -> !existing.contains(entry.getEnglishNormalized()))
                    .toList();
            if (!missing.isEmpty()) {
                repository.saveAll(missing);
                if (log.isInfoEnabled()) {
                    log.info("Seeded {} offline translation entries", missing.size());
                }
            }
        } catch (IOException | tools.jackson.core.JacksonException exception) {
            throw new IllegalStateException("Could not load the translation word bank", exception);
        }
    }

    private static TranslationEntry prepare(TranslationEntry entry) {
        entry.setId(null);
        entry.setEnglishNormalized(TranslationServiceImpl.normalize(entry.getEnglish()));
        entry.setSinhalaNormalized(TranslationServiceImpl.normalize(entry.getSinhala()));
        entry.setTransliterationNormalized(TranslationServiceImpl.normalize(entry.getTransliteration()));
        entry.setPronunciationNormalized(TranslationServiceImpl.normalize(entry.getPronunciation()));
        entry.setAliasesNormalized(entry.getAliases().stream().map(TranslationServiceImpl::normalize).toList());
        entry.setActive(true);
        return entry;
    }
}
