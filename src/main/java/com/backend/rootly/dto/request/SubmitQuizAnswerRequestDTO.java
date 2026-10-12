package com.backend.rootly.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitQuizAnswerRequestDTO {

    @NotBlank
    private String questionId;

    private String answer;

    private Boolean timedOut;

    public boolean isTimedOut() {
        return Boolean.TRUE.equals(timedOut);
    }
}
