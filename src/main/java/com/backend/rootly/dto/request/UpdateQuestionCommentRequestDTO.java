package com.backend.rootly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateQuestionCommentRequestDTO {

    @NotBlank(message = "body is required")
    @Size(max = 5_000, message = "body must not exceed 5000 characters")
    private String body;
}
