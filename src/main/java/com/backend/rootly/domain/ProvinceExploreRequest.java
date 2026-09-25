package com.backend.rootly.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvinceExploreRequest {
    private String provinceId;
    private String q = "";
    private Integer page = 0;
    private Integer size = 10;

    public ProvinceExploreRequest(String provinceId, Integer page, Integer size) {
        this(provinceId, "", page, size);
    }
}
