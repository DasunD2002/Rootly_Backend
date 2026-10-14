package com.backend.rootly.dto.response;

import com.backend.rootly.entity.UnlockCondition;
import com.backend.rootly.enums.CapsulePrivacy;
import com.backend.rootly.enums.CapsuleStatus;
import com.backend.rootly.enums.CapsuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapsuleResponseDTO {

    private String id;
    private String creatorId;
    private String title;
    private String description;
    private String coverPhotoUrl;
    private CapsuleType type;
    private UnlockCondition unlockCondition;
    private CapsulePrivacy privacy;
    private List<String> sharedWithUserIds;
    private List<String> contributorIds;
    private CapsuleStatus status;
    private String chainedFromCapsuleId;
    private Instant createdAt;
    private Instant updatedAt;
}
