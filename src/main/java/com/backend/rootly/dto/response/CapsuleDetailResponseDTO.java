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
@SuppressWarnings("PMD.TooManyFields")
public class CapsuleDetailResponseDTO {

    private String id;
    private String creatorId;
    private String creatorName;
    private String title;
    private String description;
    private String coverPhotoUrl;
    private CapsuleType category;
    private CapsuleStatus status;
    private UnlockCondition unlockCondition;
    private Instant unlockDate;
    private String countdown;
    private CapsulePrivacy privacy;
    private Boolean allowMemberContributions;
    private List<ContributorDTO> contributors;
    private List<MemoryResponseDTO> memories;
    private Instant createdAt;
    private Instant updatedAt;
}
