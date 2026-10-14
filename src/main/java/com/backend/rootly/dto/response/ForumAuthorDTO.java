package com.backend.rootly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumAuthorDTO {

    private String id;
    private String name;
    private String initials;
    private String role;
    private String photoUrl;
    private boolean verified;
}
