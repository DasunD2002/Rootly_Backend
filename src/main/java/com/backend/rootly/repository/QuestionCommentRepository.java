package com.backend.rootly.repository;

import com.backend.rootly.entity.QuestionComment;
import com.backend.rootly.enums.CommentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionCommentRepository extends MongoRepository<QuestionComment, String> {

    Optional<QuestionComment> findByIdAndStatus(String id, CommentStatus status);

    List<QuestionComment> findByQuestionIdAndStatusOrderByCreatedAtAsc(String questionId, CommentStatus status);

    List<QuestionComment> findByQuestionIdOrderByCreatedAtAsc(String questionId);
}
