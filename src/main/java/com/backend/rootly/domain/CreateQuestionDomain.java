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
public class CreateQuestionDomain {

    private String title;
    private String body;
    private String location;
    private String placeId;
    private QuestionCategory category;
}
