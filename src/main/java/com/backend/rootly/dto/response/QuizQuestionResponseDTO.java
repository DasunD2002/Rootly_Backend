package com.backend.rootly.dto.response;

import com.backend.rootly.enums.QuizType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionResponseDTO {

    private String id;
    private QuizType type;
    private String prompt;
    private String displayWord;
    private List<String> letters;
    private List<String> options;
    private List<Integer> blankPositions;
    private int targetLength;
    private int maximumPoints;
    private int timeLimitSeconds;
    private Instant deadlineAt;
}
