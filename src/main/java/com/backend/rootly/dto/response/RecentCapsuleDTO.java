package com.backend.rootly.dto.response;

import com.backend.rootly.enums.CapsuleStatus;
import com.backend.rootly.enums.CapsuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentCapsuleDTO {

    private String id;
    private String title;
    private CapsuleType category;
    private CapsuleStatus status;
    private String coverPhotoUrl;
    private Instant unlockDate;
    private String countdown;
    private long memoryCount;
}
