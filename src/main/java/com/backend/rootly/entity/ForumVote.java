package com.backend.rootly.entity;

import com.backend.rootly.enums.ForumTargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "forumVotes")
@CompoundIndex(name = "forum_vote_user_target_uq",
        def = "{'userId': 1, 'targetType': 1, 'targetId': 1}", unique = true)
public class ForumVote {

    @Id
    private String id;

    @NotBlank
    @Field("userId")
    private String userId;

    @NotNull
    @Field("targetType")
    private ForumTargetType targetType;

    @NotBlank
    @Field("targetId")
    private String targetId;

    @Min(-1)
    @Max(1)
    @Field("value")
    private int value;

    @NotNull
    @Field("createdAt")
    private Instant createdAt;

    @NotNull
    @Field("updatedAt")
    private Instant updatedAt;
}
