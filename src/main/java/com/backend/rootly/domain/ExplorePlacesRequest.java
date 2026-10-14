package com.backend.rootly.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExplorePlacesRequest {

    private String q = "";
    private String category = "all";
    private int page;
    private int size = 20;
}
