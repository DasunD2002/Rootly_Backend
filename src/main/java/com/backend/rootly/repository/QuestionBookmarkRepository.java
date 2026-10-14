package com.backend.rootly.repository;

import com.backend.rootly.entity.QuestionBookmark;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionBookmarkRepository extends MongoRepository<QuestionBookmark, String> {

    Optional<QuestionBookmark> findByUserIdAndQuestionId(String userId, String questionId);

    List<QuestionBookmark> findByUserIdAndQuestionIdIn(String userId, Collection<String> questionIds);
}
