package com.backend.rootly.dto.request;

import com.backend.rootly.enums.CapsuleEntryType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMemoryRequestDTO {

    @NotNull(message = "type is required")
    private CapsuleEntryType type;

    private String title;

    private String description;

    private String mediaUrl;

    private String textContent;

    private Instant unlockDate;
}
