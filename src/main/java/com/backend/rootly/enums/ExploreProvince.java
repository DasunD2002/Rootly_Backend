package com.backend.rootly.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum ExploreProvince {
    NORTHERN("northern", "Q598745"),
    NORTH_CENTRAL("north-central", "Q1057124"),
    NORTH_WESTERN("north-western", "Q876339"),
    CENTRAL("central", "Q190716"),
    EASTERN("eastern", "Q1046126"),
    WESTERN("western", "Q856686"),
    SOUTHERN("southern", "Q876308"),
    SABARAGAMUWA("sabaragamuwa", "Q853272"),
    UVA("uva", "Q876293");

    private final String id;
    private final String wikidataId;

    public static ExploreProvince fromId(String id) {
        String normalized = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(value -> value.id.equals(normalized))
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "provinceId must identify one of Sri Lanka's nine provinces"));
    }
}
