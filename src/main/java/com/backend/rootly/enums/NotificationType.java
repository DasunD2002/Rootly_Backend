package com.backend.rootly.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationType {
    MEMORY_ADDED("Memory Added"),
    WEEKLY_DIGEST("Weekly Digest"),
    SECURITY_CHECK("Security Check"),
    CAPSULE_INVITE("Capsule Invite"),
    CAPSULE_UNLOCKED("Capsule Unlocked");

    private final String value;

    NotificationType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static NotificationType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (NotificationType type : values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown notification type: " + value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
