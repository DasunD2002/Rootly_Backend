package com.backend.rootly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionPageResponseDTO {

    private List<QuestionSummaryDTO> items;
    private int page;
    private int size;
    private long total;
    private boolean hasNext;
}
