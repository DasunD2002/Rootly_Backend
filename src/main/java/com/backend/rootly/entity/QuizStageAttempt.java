package com.backend.rootly.entity;

import com.backend.rootly.enums.QuizType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.TooManyFields")
public class QuizStageAttempt {

    @Field("questionId")
    private String questionId;

    @Field("type")
    private QuizType type;

    @Field("prompt")
    private String prompt;

    @Field("answer")
    private String answer;

    @Field("displayWord")
    private String displayWord;

    @Builder.Default
    @Field("letters")
    private List<String> letters = new ArrayList<>();

    @Builder.Default
    @Field("options")
    private List<String> options = new ArrayList<>();

    @Builder.Default
    @Field("blankPositions")
    private List<Integer> blankPositions = new ArrayList<>();

    @Field("maximumPoints")
    private int maximumPoints;

    @Field("issuedAt")
    private Instant issuedAt;

    @Field("deadlineAt")
    private Instant deadlineAt;

    @Field("attemptCount")
    private int attemptCount;

    @Field("correct")
    private boolean correct;

    @Field("timedOut")
    private boolean timedOut;

    @Field("pointsAwarded")
    private int pointsAwarded;

    @Field("completedAt")
    private Instant completedAt;
}
