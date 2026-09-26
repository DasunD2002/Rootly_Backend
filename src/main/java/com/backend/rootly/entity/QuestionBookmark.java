package com.backend.rootly.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "questionBookmarks")
@CompoundIndex(name = "question_bookmark_user_question_uq",
        def = "{'userId': 1, 'questionId': 1}", unique = true)
public class QuestionBookmark {

    @Id
    private String id;

    @NotBlank
    @Field("userId")
    private String userId;

    @NotBlank
    @Field("questionId")
    private String questionId;

    @NotNull
    @Field("createdAt")
    private Instant createdAt;
}
