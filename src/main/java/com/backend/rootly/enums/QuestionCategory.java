package com.backend.rootly.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum QuestionCategory {
    RITUALS_ETIQUETTE("rituals-etiquette", "Rituals & Etiquette"),
    ARCHITECTURE("architecture", "Architecture"),
    LANGUAGE("language", "Language"),
    HISTORY("history", "History"),
    CRAFTS("crafts", "Crafts"),
    GETTING_THERE("getting-there", "Getting There"),
    FOLKLORE("folklore", "Folklore");

    private final String slug;
    private final String displayName;

    QuestionCategory(String slug, String displayName) {
        this.slug = slug;
        this.displayName = displayName;
    }

    @JsonValue
    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static QuestionCategory fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(category -> category.slug.equalsIgnoreCase(normalized)
                        || category.name().equalsIgnoreCase(normalized)
                        || category.displayName.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported question category: " + value));
    }
}
