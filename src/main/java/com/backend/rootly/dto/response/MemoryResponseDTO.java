package com.backend.rootly.dto.response;

import com.backend.rootly.enums.CapsuleEntryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoryResponseDTO {

    private String id;
    private String capsuleId;
    private String authorId;
    private String authorName;
    private String authorPhotoUrl;
    private CapsuleEntryType type;
    private String title;
    private String description;
    private String mediaUrl;
    private String textContent;
    private Integer likesCount;
    private Boolean isLocked;
    private Instant unlockDate;
    private Instant createdAt;
}
