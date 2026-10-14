package com.backend.rootly.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CapsuleStatus {
    OPEN("open"),
    SEALED("sealed"),
    UNLOCKED("unlocked");

    private final String value;

    CapsuleStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
