package com.backend.rootly.repository;

import com.backend.rootly.entity.QuizSession;
import com.backend.rootly.enums.QuizSessionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizSessionRepository extends MongoRepository<QuizSession, String> {

    Optional<QuizSession> findFirstByUserIdAndStatusOrderByStartedAtDesc(
            String userId, QuizSessionStatus status);

    Optional<QuizSession> findByIdAndUserId(String id, String userId);

    List<QuizSession> findByUserIdOrderByStartedAtDesc(String userId);

    List<QuizSession> findByStatusAndCompletedAtBetween(
            QuizSessionStatus status, Instant start, Instant end);
}
