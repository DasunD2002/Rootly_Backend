package com.backend.rootly.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegister {

    private String name;
    private String email;
    private String phone;
    private String gender;
    private String password;
    private String photoUrl;
    private String district;
    private List<String> languages;
}
