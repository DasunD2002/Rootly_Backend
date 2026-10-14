package com.backend.rootly.repository;

import com.backend.rootly.entity.ForumVote;
import com.backend.rootly.enums.ForumTargetType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ForumVoteRepository extends MongoRepository<ForumVote, String> {

    Optional<ForumVote> findByUserIdAndTargetTypeAndTargetId(
            String userId, ForumTargetType targetType, String targetId);

    List<ForumVote> findByUserIdAndTargetTypeAndTargetIdIn(
            String userId, ForumTargetType targetType, Collection<String> targetIds);
}
