package com.backend.rootly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExplorePlaceDTO {

    private String id;
    private String name;
    private String subtitle;
    private String category;
    private String categoryId;
    private ExploreLocationDTO location;
    private String description;
    private String imageUrl;
    private String imageSourceUrl;
    private String sourceUrl;
    private String wikipediaUrl;
}
