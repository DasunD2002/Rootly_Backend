package com.backend.rootly.entity;

import com.backend.rootly.enums.UnlockConditionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Clock;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnlockCondition {

    @Field("type")
    private UnlockConditionType type;

    @Field("date")
    private Instant date;

    @Field("location")
    private String location;

    @Field("occasionName")
    private String occasionName;

    public boolean hasUnlocked() {
        return type == UnlockConditionType.DATE && date != null && !date.isAfter(Instant.now());
    }

    public void validateDateSealing() {
        if (type != UnlockConditionType.DATE) {
            throw new IllegalArgumentException("Set a future unlock date in Edit Capsule Details first.");
        }
        validate(Clock.systemUTC());
    }

    public void validate(Clock clock) {
        if (this.type == null) {
            throw new IllegalArgumentException("unlockCondition.type is required");
        }
        if (this.type == UnlockConditionType.DATE) {
            if (this.date == null || !this.date.isAfter(Instant.now(clock))) {
                throw new IllegalArgumentException("unlockCondition.date must be a future date");
            }
        } else if (this.type == UnlockConditionType.LOCATION) {
            requireText(this.location, "unlockCondition.location is required for a location unlock");
        } else if (this.type == UnlockConditionType.OCCASION) {
            requireText(this.occasionName, "unlockCondition.occasionName is required for an occasion unlock");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
