package com.backend.rootly.dto.request;

import com.backend.rootly.entity.UnlockCondition;
import com.backend.rootly.enums.CapsulePrivacy;
import com.backend.rootly.enums.CapsuleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateCapsuleRequestDTO {

    private String creatorId;

    @Size(max = 150, message = "title must not exceed 150 characters")
    private String title;

    @Size(max = 2_000, message = "description must not exceed 2000 characters")
    private String description;

    private String coverPhotoUrl;

    @NotNull(message = "type is required")
    private CapsuleType type;

    @NotNull(message = "unlockCondition is required")
    @Valid
    private UnlockCondition unlockCondition;

    @NotNull(message = "privacy is required")
    private CapsulePrivacy privacy;

    private List<String> sharedWithUserIds;

    private List<String> contributorIds;

    private String chainedFromCapsuleId;
}
