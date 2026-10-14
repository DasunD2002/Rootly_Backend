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
public class InviteContributorRequestDTO {

    @NotBlank(message = "contributorId is required")
    @Size(max = 100, message = "contributorId must not exceed 100 characters")
    private String contributorId;
}
