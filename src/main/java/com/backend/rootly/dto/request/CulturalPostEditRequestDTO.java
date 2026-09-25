package com.backend.rootly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record CulturalPostEditRequestDTO(

        @Size(max = 60, message = "Maximum character limit is 60")
        @NotBlank(message = "Title can't be empty")
        String title,

        @Size(max = 6000, message = "Maximum character limit is 6000")
        @NotBlank(message = "Story can't be empty")
        String story,

        @NotBlank(message = "Need to provide Video or Image")
        String media,

        @NotBlank(message = "Category can't be empty")
        String category,

        String location,

        List<String> tags,

        @NotBlank(message = "Visibility can't be empty")
        String visibility,

        List<String> proofs,

        Boolean disableComments
) {
}
