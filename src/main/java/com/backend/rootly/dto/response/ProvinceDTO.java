package com.backend.rootly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvinceDTO {
    private String id;
    private String name;
    private String description;
    private String capital;
    private List<String> districts;
    private String imageUrl;
    private String imageSourceUrl;
    private String sourceUrl;
    private String wikipediaUrl;
}
