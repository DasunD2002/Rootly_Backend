package com.backend.rootly.dto.response;

import lombok.Data;

import java.time.Instant;

@Data
public class PostCommentDTO {
    private String userId;
    private String text;
    private Instant createdAt;
    private String authorName;
    private String authorPhoto;
}
