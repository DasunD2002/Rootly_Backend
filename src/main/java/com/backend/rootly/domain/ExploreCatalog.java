package com.backend.rootly.domain;

import com.backend.rootly.dto.response.ExplorePlaceDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExploreCatalog {

    private List<ExplorePlaceDTO> places;
    private Instant fetchedAt;
    private boolean truncated;
}
