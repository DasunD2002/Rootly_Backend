package com.backend.rootly.repository;

import com.backend.rootly.entity.QuizQuestion;
import com.backend.rootly.enums.QuizType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizQuestionRepository extends MongoRepository<QuizQuestion, String> {

    List<QuizQuestion> findAllByTypeAndActiveTrue(QuizType type);

    Optional<QuizQuestion> findByCode(String code);
}
