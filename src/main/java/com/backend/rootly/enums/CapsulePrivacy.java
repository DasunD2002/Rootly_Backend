package com.backend.rootly.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum CapsulePrivacy {
    PRIVATE("private"),
    SHARED("shared"),
    PUBLIC_AFTER_UNLOCK("public_after_unlock");

    private final String value;

    CapsulePrivacy(String value) {
        this.value = value;
    }

    @JsonCreator
    public static CapsulePrivacy fromValue(String value) {
        for (CapsulePrivacy privacy : values()) {
            if (privacy.value.equalsIgnoreCase(value)) {
                return privacy;
            }
        }
        throw new IllegalArgumentException("Unknown capsule privacy: " + value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
