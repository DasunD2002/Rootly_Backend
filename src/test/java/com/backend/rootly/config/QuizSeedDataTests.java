package com.backend.rootly.config;

import com.backend.rootly.entity.QuizQuestion;
import com.backend.rootly.enums.QuizType;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuizSeedDataTests {

    @Test
    void containsAReusableBalancedQuestionBank() throws Exception {
        List<QuizQuestion> questions;
        try (InputStream input = new ClassPathResource("data/quizzes.json").getInputStream()) {
            questions = Arrays.asList(JsonMapper.builder().build()
                    .readValue(input, QuizQuestion[].class));
        }

        assertThat(questions).hasSize(18);
        assertThat(questions).extracting(QuizQuestion::getCode).doesNotHaveDuplicates();
        assertThat(questions).filteredOn(question -> question.getType() == QuizType.WORD_BUILDER).hasSize(6);
        assertThat(questions).filteredOn(question -> question.getType() == QuizType.MEANING_MATCH).hasSize(6);
        assertThat(questions).filteredOn(question -> question.getType() == QuizType.FILL_LETTERS).hasSize(6);
        assertThat(questions).allSatisfy(question -> {
            assertThat(question.getPrompt()).isNotBlank();
            assertThat(question.getAnswer()).isNotBlank();
            assertThat(question.getMaximumPoints()).isPositive();
            assertThat(question.isActive()).isTrue();
        });
        assertThat(questions)
                .filteredOn(question -> question.getType() == QuizType.MEANING_MATCH)
                .allSatisfy(question -> assertThat(question.getOptions()).contains(question.getAnswer()));
        assertThat(questions)
                .filteredOn(question -> question.getType() == QuizType.FILL_LETTERS)
                .allSatisfy(question -> {
                    assertThat(question.getDisplayWord()).isNotBlank();
                    assertThat(question.getBlankPositions()).hasSize(question.getAnswer().length());
                    assertThat(question.getLetters()).containsAll(
                            question.getAnswer().codePoints()
                                    .mapToObj(value -> new String(Character.toChars(value))).toList());
                });
    }
}
