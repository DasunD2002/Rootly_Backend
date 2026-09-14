package com.backend.rootly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationGroupDTO {

    private List<NotificationResponseDTO> today;
    private List<NotificationResponseDTO> yesterday;
    private List<NotificationResponseDTO> earlier;
    private long unreadCount;
}
