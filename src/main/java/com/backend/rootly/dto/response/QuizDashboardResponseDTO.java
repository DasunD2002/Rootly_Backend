package com.backend.rootly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizDashboardResponseDTO {

    private int completedSteps;
    private int todayScore;
    private int streakDays;
    private String activeSessionId;
    private List<Boolean> weekActivity;
    private List<QuizLeaderResponseDTO> weeklyLeaders;
}
