package com.backend.rootly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswerResponseDTO {

    private boolean correct;
    private boolean timedOut;
    private boolean completed;
    private String feedback;
    private int pointsAwarded;
    private int totalScore;
    private int completedSteps;
    private QuizSessionResponseDTO session;
}
