package com.backend.rootly.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequestDTO {
    private String name;
    private String handle;
    private String bio;
    private String district;
    private String photoUrl;
    private String coverUrl;
}
