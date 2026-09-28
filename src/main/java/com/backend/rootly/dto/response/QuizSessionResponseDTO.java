package com.backend.rootly.dto.response;

import com.backend.rootly.enums.QuizSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSessionResponseDTO {

    private String sessionId;
    private QuizSessionStatus status;
    private int currentStage;
    private int completedSteps;
    private int totalScore;
    private QuizQuestionResponseDTO question;
}
