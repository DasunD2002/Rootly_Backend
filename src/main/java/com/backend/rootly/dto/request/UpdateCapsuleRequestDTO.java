package com.backend.rootly.dto.request;

import com.backend.rootly.entity.UnlockCondition;
import com.backend.rootly.enums.CapsulePrivacy;
import com.backend.rootly.enums.CapsuleStatus;
import com.backend.rootly.enums.CapsuleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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

    @Size(max = 150, message = "title must not exceed 150 characters")
    private String title;

    @Size(max = 2_000, message = "description must not exceed 2000 characters")
    private String description;

    private String coverPhotoUrl;

    private CapsuleType type;

    @Valid
    private UnlockCondition unlockCondition;

    private CapsulePrivacy privacy;

    private List<String> sharedWithUserIds;

    private CapsuleStatus status;
}
