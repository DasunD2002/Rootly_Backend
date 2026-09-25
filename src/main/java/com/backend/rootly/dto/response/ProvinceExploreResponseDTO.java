package com.backend.rootly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvinceExploreResponseDTO {
    private ProvinceDTO province;
    private ExplorePlacesResponseDTO places;
    private List<ProvinceTraditionDTO> traditions;
}
