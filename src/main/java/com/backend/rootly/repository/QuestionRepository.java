package com.backend.rootly.repository;

import com.backend.rootly.entity.Question;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends MongoRepository<Question, String>, QuestionRepositoryCustom {
}
