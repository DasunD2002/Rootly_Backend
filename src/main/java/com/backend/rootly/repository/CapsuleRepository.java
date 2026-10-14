package com.backend.rootly.repository;

import com.backend.rootly.entity.Capsule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CapsuleRepository extends MongoRepository<Capsule, String> {
}
