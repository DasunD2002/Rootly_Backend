package com.backend.rootly.entity;

import com.backend.rootly.enums.CapsuleEntryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "capsuleEntries")
public class CapsuleEntry {

    @Id
    private String id;

    @Indexed
    @Field("capsuleId")
    private String capsuleId;

    @Field("contributorId")
    private String contributorId;

    @Field("type")
    private CapsuleEntryType type;

    @Field("content")
    private String content;

    private String caption;
    private Boolean archived;

    @CreatedDate
    @Field("createdAt")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    private Instant updatedAt;
}
