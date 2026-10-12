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
@SuppressWarnings("PMD.TooManyFields")
public class UserResponseDTO {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String gender;
    private String photoUrl;
    private String coverUrl;
    private String bio;
    private String handle;
    private String district;
    private List<String> languages;
    private String role;
    private String verificationStatus;
    private Integer followerCount;
    private Integer followingCount;
    private String profileVisibility;
    private Boolean showActivity;
}
