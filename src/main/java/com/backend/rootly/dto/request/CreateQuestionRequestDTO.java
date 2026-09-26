package com.backend.rootly.dto.request;

import com.backend.rootly.enums.QuestionCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuestionRequestDTO {

    @NotBlank(message = "title is required")
    @Size(min = 10, max = 200, message = "title must contain between 10 and 200 characters")
    private String title;

    @NotBlank(message = "body is required")
    @Size(min = 20, max = 5_000, message = "body must contain between 20 and 5000 characters")
    private String body;

    @NotBlank(message = "location is required")
    @Size(max = 120, message = "location must not exceed 120 characters")
    private String location;

    @Size(max = 120, message = "placeId must not exceed 120 characters")
    private String placeId;

    @NotNull(message = "category is required")
    private QuestionCategory category;
}
