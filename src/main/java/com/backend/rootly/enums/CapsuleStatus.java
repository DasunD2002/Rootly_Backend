package com.backend.rootly.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CapsuleStatus {
    OPEN("open"),
    SEALED("sealed"),
    UNLOCKED("unlocked"),
    LOCKED("locked"),
    OPENED("opened");

    private final String value;

    CapsuleStatus(String value) {
        this.value = value;
    }

    @JsonCreator
    public static CapsuleStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (CapsuleStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown capsule status: " + value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public boolean isLocked() {
        return this == SEALED || this == LOCKED;
    }

    public boolean isOpened() {
        return this == OPEN || this == OPENED || this == UNLOCKED;
    }
}
