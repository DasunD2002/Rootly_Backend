package com.backend.rootly.dto;

import com.backend.rootly.entity.UnlockCondition;
import com.backend.rootly.enums.CapsulePrivacy;
import com.backend.rootly.enums.CapsuleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Request payload used to create a time capsule. */
public record CreateCapsuleRequest(
        @NotBlank(message = "creatorId is required") String creatorId,
        @Size(max = 150, message = "title must not exceed 150 characters") String title,
        @Size(max = 2_000, message = "description must not exceed 2000 characters") String description,
        String coverPhotoUrl,
        @NotNull(message = "type is required") CapsuleType type,
        @NotNull(message = "unlockCondition is required") @Valid UnlockCondition unlockCondition,
        @NotNull(message = "privacy is required") CapsulePrivacy privacy,
        List<String> sharedWithUserIds,
        List<String> contributorIds,
        String chainedFromCapsuleId) {
}
