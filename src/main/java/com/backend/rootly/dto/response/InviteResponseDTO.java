package com.backend.rootly.dto.response;

import com.backend.rootly.enums.InviteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteResponseDTO {

    private String inviteToken;
    private String inviteLink;
    private String capsuleId;
    private InviteStatus status;
    private Instant expiresAt;
}
