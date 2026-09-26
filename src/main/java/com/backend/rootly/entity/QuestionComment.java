package com.backend.rootly.entity;

import com.backend.rootly.enums.CommentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "questionComments")
@CompoundIndexes({
        @CompoundIndex(name = "comment_question_parent_created_idx",
                def = "{'questionId': 1, 'parentCommentId': 1, 'createdAt': 1}"),
        @CompoundIndex(name = "comment_question_root_idx", def = "{'questionId': 1, 'rootCommentId': 1}")
})
public class QuestionComment {

    @Id
    private String id;

    @NotBlank
    @Indexed
    @Field("questionId")
    private String questionId;

    @Field("parentCommentId")
    private String parentCommentId;

    @NotBlank
    @Field("rootCommentId")
    private String rootCommentId;

    @NotBlank
    @Indexed
    @Field("authorId")
    private String authorId;

    @NotBlank
    @Size(max = 5_000)
    @Field("body")
    private String body;

    @Builder.Default
    @Field("voteScore")
    private int voteScore = 0;

    @Builder.Default
    @Field("verified")
    private boolean verified = false;

    @Builder.Default
    @Field("accepted")
    private boolean accepted = false;

    @Builder.Default
    @Field("status")
    private CommentStatus status = CommentStatus.ACTIVE;

    @NotNull
    @Field("createdAt")
    private Instant createdAt;

    @NotNull
    @Field("updatedAt")
    private Instant updatedAt;

    @Version
    private Long version;
}
