package com.backend.rootly.dto.request;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SubmitQuizAnswerRequestDTOTests {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void treatsMissingOrNullTimedOutAsFalse() throws Exception {
        SubmitQuizAnswerRequestDTO missing = jsonMapper.readValue(
                "{\"questionId\":\"question-1\",\"answer\":\"STUPA\"}",
                SubmitQuizAnswerRequestDTO.class);
        SubmitQuizAnswerRequestDTO explicitNull = jsonMapper.readValue(
                "{\"questionId\":\"question-1\",\"answer\":\"STUPA\",\"timedOut\":null}",
                SubmitQuizAnswerRequestDTO.class);

        assertThat(missing.isTimedOut()).isFalse();
        assertThat(explicitNull.isTimedOut()).isFalse();
    }

    @Test
    void retainsAnExplicitTimeout() throws Exception {
        SubmitQuizAnswerRequestDTO request = jsonMapper.readValue(
                "{\"questionId\":\"question-1\",\"timedOut\":true}",
                SubmitQuizAnswerRequestDTO.class);

        assertThat(request.isTimedOut()).isTrue();
    }
}
