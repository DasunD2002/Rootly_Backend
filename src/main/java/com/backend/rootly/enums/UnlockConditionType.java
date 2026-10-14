package com.backend.rootly.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum UnlockConditionType {
    DATE("date"),
    LOCATION("location"),
    OCCASION("occasion");

    private final String value;

    UnlockConditionType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static UnlockConditionType fromValue(String value) {
        for (UnlockConditionType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown unlock condition type: " + value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
