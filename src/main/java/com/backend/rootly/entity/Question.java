package com.backend.rootly.entity;

import com.backend.rootly.enums.QuestionCategory;
import com.backend.rootly.enums.QuestionStatus;
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
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "questions")
@CompoundIndexes({
        @CompoundIndex(name = "question_category_created_idx", def = "{'category': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "question_top_idx", def = "{'voteScore': -1, 'createdAt': -1}"),
        @CompoundIndex(name = "question_unanswered_idx", def = "{'commentCount': 1, 'createdAt': -1}")
})
public class Question {

    @Id
    private String id;

    @NotBlank
    @Size(min = 10, max = 200)
    @TextIndexed(weight = 4)
    @Field("title")
    private String title;

    @NotBlank
    @Size(min = 20, max = 5_000)
    @TextIndexed(weight = 2)
    @Field("body")
    private String body;

    @NotBlank
    @Size(max = 120)
    @TextIndexed
    @Field("location")
    private String location;

    @Field("placeId")
    private String placeId;

    @NotNull
    @Field("category")
    private QuestionCategory category;

    @NotBlank
    @Indexed
    @Field("authorId")
    private String authorId;

    @Builder.Default
    @Field("voteScore")
    private int voteScore = 0;

    @Builder.Default
    @Field("commentCount")
    private int commentCount = 0;

    @Field("acceptedCommentId")
    private String acceptedCommentId;

    @Builder.Default
    @Field("verified")
    private boolean verified = false;

    @Builder.Default
    @Field("status")
    private QuestionStatus status = QuestionStatus.OPEN;

    @NotNull
    @Field("createdAt")
    private Instant createdAt;

    @NotNull
    @Field("updatedAt")
    private Instant updatedAt;

    @Version
    private Long version;
}
