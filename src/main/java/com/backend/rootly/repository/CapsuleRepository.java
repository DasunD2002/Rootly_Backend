package com.backend.rootly.repository;

import com.backend.rootly.entity.Capsule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CapsuleRepository extends MongoRepository<Capsule, String> {

    List<Capsule> findByCreatorIdOrderByCreatedAtDesc(String creatorId);

    List<Capsule> findByCreatorIdOrContributorIdsContainingOrderByCreatedAtDesc(String creatorId, String contributorId);

    long countByCreatorIdOrContributorIdsContaining(String creatorId, String contributorId);
}
