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

    @Field("title")
    private String title;

    @Field("description")
    private String description;

    @Field("mediaUrl")
    private String mediaUrl;

    @Field("textContent")
    private String textContent;

    @Field("content")
    private String content;

    @Field("likesCount")
    private Integer likesCount;

    @Field("unlockDate")
    private Instant unlockDate;

    @CreatedDate
    @Field("createdAt")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    private Instant updatedAt;

    public String getAuthorId() {
        return contributorId;
    }

    public void setAuthorId(String authorId) {
        this.contributorId = authorId;
    }
}
