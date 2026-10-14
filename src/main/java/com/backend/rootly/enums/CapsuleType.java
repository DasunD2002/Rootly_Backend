package com.backend.rootly.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum CapsuleType {
    PERSONAL("personal"),
    FAMILY("family"),
    COMMUNITY("community");

    private final String value;

    CapsuleType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static CapsuleType fromValue(String value) {
        for (CapsuleType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown capsule type: " + value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
