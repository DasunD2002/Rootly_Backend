package com.backend.rootly.dto.response;

import lombok.Data;

import java.util.List;

@Data
@SuppressWarnings("PMD.TooManyFields")
public class CulturalPostResponseDTO {
    private String id;
    private String userId;
    private String title;
    private String story;
    private String media;
    private String category;
    private String location;
    private List<String> tags;
    private String visibility;
    private List<String> proofs;
    
    private Integer likeCount;
    private Integer shareCount;
    private Integer commentCount;
    private List<PostCommentDTO> comments;
    private Boolean disableComments;
    private String authorName;
    private String authorHandle;
    private String authorPhoto;
    private Boolean isLiked;
    private java.time.Instant createdAt;
}
