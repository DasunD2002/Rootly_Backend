package com.backend.rootly.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumVoteRequestDTO {

    @NotNull(message = "value is required")
    @Min(value = -1, message = "value must be -1, 0, or 1")
    @Max(value = 1, message = "value must be -1, 0, or 1")
    private Integer value;
}
