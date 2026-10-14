package com.backend.rootly.domain;

import com.backend.rootly.enums.QuestionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionSearchDomain {

    private String query;
    private QuestionCategory category;
    private String sort;
    private int page;
    private int size;
}
