package com.backend.rootly.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@Getter
@Setter
@Document(collection = "posts")
public class CulturalPost {

    @Id
    private String id;

    @Field("userId")
    private String userId;

    @Field("title")
    private String title;

    @Field("story")
    private String story;

    @Field("media")
    private String media;

    @Field("category")
    private String category;

    @Field("location")
    private String location;

    @Field("tags")
    private List<String> tags;

    @Field("visibility")
    private String visibility;

    @Field("proofs")
    private List<String> proofs;

    @Field("likeCount")
    private Integer likeCount;

    @Field("shareCount")
    private Integer shareCount;

    @Field("commentCount")
    private Integer commentCount;

    @Field("comments")
    private List<PostComment> comments;

    @Field("disableComments")
    private Boolean disableComments;

    @Field("likedBy")
    private List<String> likedBy;

    @org.springframework.data.annotation.CreatedDate
    @Field("createdAt")
    private java.time.Instant createdAt;
}
