package com.backend.rootly.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;

public enum TranslationCategory {
    TEMPLE("temple", "Temple"),
    GREETINGS("greetings", "Greetings"),
    FOOD("food", "Food"),
    DIRECTIONS("directions", "Directions");

    private final String id;
    private final String label;

    TranslationCategory(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static TranslationCategory fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("category is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(category -> category.id.equals(normalized)
                        || category.label.toLowerCase(Locale.ROOT).equals(normalized)
                        || category.name().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "category must be temple, greetings, food, or directions"));
    }
}
