package com.backend.rootly.entity;

import com.backend.rootly.enums.TranslationCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "translations")
@CompoundIndexes({
        @CompoundIndex(name = "translation_category_english_idx",
                def = "{'active': 1, 'category': 1, 'englishNormalized': 1}"),
        @CompoundIndex(name = "translation_sinhala_idx",
                def = "{'active': 1, 'sinhalaNormalized': 1}")
})
@SuppressWarnings("PMD.TooManyFields")
public class TranslationEntry {

    @Id
    private String id;

    @NotBlank
    @Size(max = 120)
    @Field("english")
    private String english;

    @NotBlank
    @Indexed(unique = true)
    @Field("englishNormalized")
    private String englishNormalized;

    @NotBlank
    @Size(max = 120)
    @Field("sinhala")
    private String sinhala;

    @NotBlank
    @Field("sinhalaNormalized")
    private String sinhalaNormalized;

    @NotBlank
    @Size(max = 160)
    @Field("transliteration")
    private String transliteration;

    @NotBlank
    @Field("transliterationNormalized")
    private String transliterationNormalized;

    @NotBlank
    @Size(max = 160)
    @Field("pronunciation")
    private String pronunciation;

    @NotBlank
    @Field("pronunciationNormalized")
    private String pronunciationNormalized;

    @Valid
    @NotEmpty
    @Builder.Default
    @Field("meanings")
    private List<TranslationMeaning> meanings = new ArrayList<>();

    @NotBlank
    @Size(max = 500)
    @Field("exampleSentence")
    private String exampleSentence;

    @NotEmpty
    @Builder.Default
    @Field("relatedWords")
    private List<String> relatedWords = new ArrayList<>();

    @Builder.Default
    @Field("aliases")
    private List<String> aliases = new ArrayList<>();

    @Builder.Default
    @Field("aliasesNormalized")
    private List<String> aliasesNormalized = new ArrayList<>();

    @NotNull
    @Field("category")
    private TranslationCategory category;

    @Builder.Default
    @NotNull
    @Field("active")
    private Boolean active = true;
}
