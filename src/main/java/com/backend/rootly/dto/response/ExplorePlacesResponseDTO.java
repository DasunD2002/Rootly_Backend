package com.backend.rootly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExplorePlacesResponseDTO {

    private List<ExplorePlaceDTO> items;
    private int page;
    private int size;
    private int total;
    private boolean hasNext;
    private String source;
    private Instant fetchedAt;
    private boolean stale;
    private boolean truncated;
}
