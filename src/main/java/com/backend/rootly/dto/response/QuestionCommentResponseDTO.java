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
public class QuestionCommentResponseDTO {

    private String id;
    private String questionId;
    private String parentCommentId;
    private ForumAuthorDTO author;
    private String body;
    private int voteScore;
    private int viewerVote;
    private boolean verified;
    private boolean accepted;
    private boolean deleted;
    private boolean ownedByViewer;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<QuestionCommentResponseDTO> replies = new ArrayList<>();
}
