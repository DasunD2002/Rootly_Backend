package com.backend.rootly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class QuestionSummaryDTO {

    private String id;
    private String title;
    private String body;
    private String location;
    private String placeId;
    private String category;
    private String categoryId;
    private ForumAuthorDTO author;
    private int voteScore;
    private int commentCount;
    private boolean verified;
    private int viewerVote;
    private boolean bookmarked;
    private boolean ownedByViewer;
    private Instant createdAt;
    private Instant updatedAt;
}
