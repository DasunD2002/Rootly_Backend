package com.backend.rootly.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CapsuleEntryType {
    VOICE("voice"),
    VIDEO("video"),
    PHOTO("photo"),
    LETTER("letter");

    private final String value;

    CapsuleEntryType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
