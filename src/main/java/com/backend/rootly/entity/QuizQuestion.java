package com.backend.rootly.entity;

import com.backend.rootly.enums.QuizType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "quiz_questions")
@CompoundIndex(name = "quiz_type_active_idx", def = "{'type': 1, 'active': 1}")
public class QuizQuestion {

    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true)
    @Field("code")
    private String code;

    @NotNull
    @Field("type")
    private QuizType type;

    @NotBlank
    @Field("prompt")
    private String prompt;

    @NotBlank
    @Field("answer")
    private String answer;

    @Field("displayWord")
    private String displayWord;

    @Builder.Default
    @Field("letters")
    private List<String> letters = new ArrayList<>();

    @Builder.Default
    @Field("options")
    private List<String> options = new ArrayList<>();

    @Builder.Default
    @Field("blankPositions")
    private List<Integer> blankPositions = new ArrayList<>();

    @Field("maximumPoints")
    private int maximumPoints;

    @Builder.Default
    @Field("active")
    private boolean active = true;
}
