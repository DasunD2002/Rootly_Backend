package com.backend.rootly.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationMeaning {

    @NotBlank
    @Size(max = 40)
    @Field("partOfSpeech")
    private String partOfSpeech;

    @NotBlank
    @Size(max = 500)
    @Field("text")
    private String text;
}
