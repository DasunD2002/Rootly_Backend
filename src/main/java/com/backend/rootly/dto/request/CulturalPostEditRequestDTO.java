package com.backend.rootly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record CulturalPostEditRequestDTO(

        @Size(max = 60, message = "Maximum character limit is 60")
        String title,

        @Size(max = 6000, message = "Maximum character limit is 6000")
        String story,

        String media,

        String category,

        String location,

        List<String> tags,

        @NotBlank(message = "Visibility can't be empty")
        String visibility,

        List<String> proofs,

        Boolean disableComments,
        Boolean isDraft
) {
}
