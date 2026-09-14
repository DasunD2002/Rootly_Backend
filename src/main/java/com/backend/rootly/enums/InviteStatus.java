package com.backend.rootly.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum InviteStatus {
    PENDING("pending"),
    ACCEPTED("accepted"),
    DECLINED("declined"),
    EXPIRED("expired");

    private final String value;

    InviteStatus(String value) {
        this.value = value;
    }

    @JsonCreator
    public static InviteStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (InviteStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown invite status: " + value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
