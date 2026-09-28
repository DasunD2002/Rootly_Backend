package com.backend.rootly.config;

import com.backend.rootly.entity.QuizQuestion;
import com.backend.rootly.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Component
@Log4j2
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rootly.quiz.seed-enabled", havingValue = "true", matchIfMissing = true)
public class QuizDataSeeder implements ApplicationRunner {

    private final QuizQuestionRepository repository;
    private final JsonMapper jsonMapper;

    @Value("${rootly.quiz.seed-resource:classpath:data/quizzes.json}")
    private Resource seedResource;

    @Override
    public void run(ApplicationArguments arguments) {
        try (InputStream input = seedResource.getInputStream()) {
            List<QuizQuestion> seeds = Arrays.asList(jsonMapper.readValue(input, QuizQuestion[].class));
            List<QuizQuestion> missing = seeds.stream()
                    .filter(question -> repository.findByCode(question.getCode()).isEmpty())
                    .peek(question -> {
                        question.setId(null);
                        question.setActive(true);
                    })
                    .toList();
            if (!missing.isEmpty()) {
                repository.saveAll(missing);
                if (log.isInfoEnabled()) {
                    log.info("Seeded {} quiz questions", missing.size());
                }
            }
        } catch (IOException | tools.jackson.core.JacksonException exception) {
            throw new IllegalStateException("Could not load the quiz question bank", exception);
        }
    }
}
