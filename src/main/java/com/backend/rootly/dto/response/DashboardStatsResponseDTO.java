package com.backend.rootly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponseDTO {

    private String userId;
    private String name;
    private String email;
    private String photoUrl;
    private long capsuleCount;
    private long memoryCount;
    private Integer subscriptionMonthsLeft;
    private List<RecentCapsuleDTO> recentCapsules;
}
