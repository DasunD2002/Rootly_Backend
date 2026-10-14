package com.backend.rootly.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "capsuleReactions")
public class CapsuleReaction {

    @Id
    private String id;

    @Indexed
    @Field("capsuleId")
    private String capsuleId;

    @Field("userId")
    private String userId;

    @Field("text")
    private String text;

    @Field("emoji")
    private String emoji;

    @CreatedDate
    @Field("createdAt")
    private Instant createdAt;
}
