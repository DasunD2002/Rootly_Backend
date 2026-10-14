package com.backend.rootly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExploreLocationDTO {

    private double latitude;
    private double longitude;
}
