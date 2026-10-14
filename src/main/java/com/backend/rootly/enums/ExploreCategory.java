package com.backend.rootly.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;

/** Filters describe source classifications, not invented historical eras. */
@Getter
@RequiredArgsConstructor
public enum ExploreCategory {
    ANCIENT_RUINS("ancient-ruins", "Ancient Ruins"),
    SACRED_SITES("sacred-sites", "Sacred Sites"),
    MUSEUMS("museums", "Museums"),
    HERITAGE_SITES("heritage-sites", "Heritage Sites");

    private final String id;
    private final String label;

    public static ExploreCategory fromId(String id) {
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(value -> value.id.equals(normalized))
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "category must be all, ancient-ruins, sacred-sites, museums or heritage-sites"));
    }
}
