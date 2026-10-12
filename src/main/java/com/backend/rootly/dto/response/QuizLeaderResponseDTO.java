package com.backend.rootly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizLeaderResponseDTO {

    private int rank;
    private String userId;
    private String name;
    private String initials;
    private int score;
    private boolean viewer;
}
