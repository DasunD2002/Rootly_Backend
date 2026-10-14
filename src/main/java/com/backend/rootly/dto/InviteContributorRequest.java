package com.backend.rootly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payload used to invite a user to contribute to a capsule. */
public record InviteContributorRequest(
        @NotBlank(message = "contributorId is required")
        @Size(max = 100, message = "contributorId must not exceed 100 characters")
        String contributorId
) {
}

