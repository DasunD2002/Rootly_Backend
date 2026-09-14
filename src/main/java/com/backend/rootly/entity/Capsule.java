package com.backend.rootly.entity;

import com.backend.rootly.enums.CapsulePrivacy;
import com.backend.rootly.enums.CapsuleStatus;
import com.backend.rootly.enums.CapsuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "capsules")
@CompoundIndex(name = "unlock_condition_date_idx", def = "{'unlockCondition.date': 1}")
@SuppressWarnings("PMD.TooManyFields")
public class Capsule {

    @Id
    private String id;

    @Indexed
    @Field("creatorId")
    private String creatorId;

    @Field("title")
    private String title;

    @Field("description")
    private String description;

    @Field("coverPhotoUrl")
    private String coverPhotoUrl;

    @Field("type")
    private CapsuleType type;

    @Field("unlockCondition")
    private UnlockCondition unlockCondition;

    @Field("privacy")
    private CapsulePrivacy privacy;

    @Field("allowMemberContributions")
    private Boolean allowMemberContributions;

    @Field("sharedWithUserIds")
    private List<String> sharedWithUserIds;

    @Indexed
    @Field("contributorIds")
    private List<String> contributorIds;

    @Indexed
    @Field("status")
    private CapsuleStatus status;

    @Field("sealedAt")
    private Instant sealedAt;

    @Field("unlockedAt")
    private Instant unlockedAt;

    @Field("chainedFromCapsuleId")
    private String chainedFromCapsuleId;

    @CreatedDate
    @Field("createdAt")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    private Instant updatedAt;

    public void validateForInvitation(String contributorId, List<String> cleanedContributorIds) {
        if (this.status != CapsuleStatus.OPEN) {
            throw new IllegalStateException("Contributors can only be invited to an open capsule");
        }
        if (contributorId.equals(this.creatorId)) {
            throw new IllegalArgumentException("The capsule creator is already a contributor");
        }
        if (cleanedContributorIds.contains(contributorId)) {
            throw new IllegalStateException("User is already a contributor to this capsule");
        }
    }
}
