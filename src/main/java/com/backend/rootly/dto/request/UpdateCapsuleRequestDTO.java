package com.backend.rootly.dto.request;

import com.backend.rootly.entity.UnlockCondition;
import com.backend.rootly.enums.CapsulePrivacy;
import com.backend.rootly.enums.CapsuleStatus;
import com.backend.rootly.enums.CapsuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCapsuleRequestDTO {

    private String title;
    private String description;
    private String coverPhotoUrl;
    private CapsuleType type;
    private CapsuleStatus status;
    private CapsulePrivacy privacy;
    private UnlockCondition unlockCondition;
    private Boolean allowMemberContributions;
    private List<String> sharedWithUserIds;
}
