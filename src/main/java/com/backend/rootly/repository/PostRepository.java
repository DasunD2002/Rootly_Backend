package com.backend.rootly.repository;

import com.backend.rootly.entity.CulturalPost;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PostRepository extends MongoRepository<CulturalPost, String> {
    java.util.List<CulturalPost> findByUserId(String userId, org.springframework.data.domain.Sort sort);
}
