package com.backend.rootly.repository;

import com.backend.rootly.entity.TranslationEntry;
import com.backend.rootly.enums.TranslationCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TranslationEntryRepository extends MongoRepository<TranslationEntry, String> {

    Optional<TranslationEntry> findByEnglishNormalized(String englishNormalized);

    @Query("{'active': true, '$or': ["
            + "{'englishNormalized': ?0}, {'aliasesNormalized': ?0}]} ")
    Optional<TranslationEntry> findActiveEnglishLookup(String normalizedText);

    @Query("{'active': true, '$or': ["
            + "{'sinhalaNormalized': ?0}, {'transliterationNormalized': ?0}, "
            + "{'pronunciationNormalized': ?0}]} ")
    Optional<TranslationEntry> findActiveSinhalaLookup(String normalizedText);

    Page<TranslationEntry> findAllByActiveTrue(Pageable pageable);

    Page<TranslationEntry> findAllByActiveTrueAndCategory(TranslationCategory category, Pageable pageable);
}
