package com.backend.rootly.domain;

import com.backend.rootly.dto.response.ProvinceDTO;
import com.backend.rootly.dto.response.ProvinceTraditionDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvinceCatalog {
    private ProvinceDTO province;
    private ExploreCatalog places;
    private List<ProvinceTraditionDTO> traditions;
}
