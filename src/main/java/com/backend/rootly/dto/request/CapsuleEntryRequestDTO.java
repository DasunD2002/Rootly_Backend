package com.backend.rootly.dto.request;

import com.backend.rootly.enums.CapsuleEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CapsuleEntryRequestDTO(
        @NotNull CapsuleEntryType type,
        @NotBlank @Size(max = 30000) String content,
        @Size(max = 1000) String caption) {
}
