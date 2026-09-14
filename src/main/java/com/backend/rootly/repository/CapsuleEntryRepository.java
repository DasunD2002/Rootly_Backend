package com.backend.rootly.repository;

import com.backend.rootly.entity.CapsuleEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CapsuleEntryRepository extends MongoRepository<CapsuleEntry, String> {

    List<CapsuleEntry> findByCapsuleIdOrderByCreatedAtDesc(String capsuleId);

    long countByContributorId(String contributorId);

    long countByCapsuleId(String capsuleId);

    void deleteByCapsuleId(String capsuleId);
}
