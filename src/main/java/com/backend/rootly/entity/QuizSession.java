package com.backend.rootly.entity;

import com.backend.rootly.enums.QuizSessionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "quiz_sessions")
@CompoundIndexes({
        @CompoundIndex(name = "quiz_user_status_started_idx", def = "{'userId': 1, 'status': 1, 'startedAt': -1}"),
        @CompoundIndex(name = "quiz_status_completed_idx", def = "{'status': 1, 'completedAt': -1}")
})
public class QuizSession {

    @Id
    private String id;

    @NotBlank
    @Field("userId")
    private String userId;

    @NotBlank
    @Field("userName")
    private String userName;

    @NotNull
    @Field("status")
    private QuizSessionStatus status;

    @Field("currentStage")
    private int currentStage;

    @Field("totalScore")
    private int totalScore;

    @NotNull
    @Field("startedAt")
    private Instant startedAt;

    @Field("completedAt")
    private Instant completedAt;

    @Builder.Default
    @Field("stages")
    private List<QuizStageAttempt> stages = new ArrayList<>();

    @Version
    private Long version;
}
