package com.backend.rootly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class QuestionDetailDTO {

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
    private String acceptedCommentId;
    private boolean verified;
    private int viewerVote;
    private boolean bookmarked;
    private boolean ownedByViewer;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<QuestionCommentResponseDTO> comments = new ArrayList<>();
}
