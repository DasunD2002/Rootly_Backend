package com.backend.rootly.domain;

import com.backend.rootly.entity.UnlockCondition;
import com.backend.rootly.enums.CapsulePrivacy;
import com.backend.rootly.enums.CapsuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCapsuleDomain {

    private String creatorId;
    private String title;
    private String description;
    private String coverPhotoUrl;
    private CapsuleType type;
    private UnlockCondition unlockCondition;
    private CapsulePrivacy privacy;
    private List<String> sharedWithUserIds;
    private List<String> contributorIds;
    private String chainedFromCapsuleId;

    public void validateUnlockCondition(Clock clock) {
        if (unlockCondition != null) {
            unlockCondition.validate(clock);
        }
    }
}
