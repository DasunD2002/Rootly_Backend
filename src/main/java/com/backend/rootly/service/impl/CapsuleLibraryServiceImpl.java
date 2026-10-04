package com.backend.rootly.service.impl;

import com.backend.rootly.dto.request.CreateCapsuleRequestDTO;
import com.backend.rootly.entity.Capsule;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.service.CapsuleLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CapsuleLibraryServiceImpl implements CapsuleLibraryService {
    private final MongoTemplate mongoTemplate;

    @Override
    public List<Capsule> list(UserReg user) {
        String userId = userId(user);
        Query query = Query.query(new Criteria().orOperator(Criteria.where("creatorId").is(userId),
                Criteria.where("contributorIds").is(userId), Criteria.where("sharedWithUserIds").is(userId))).addCriteria(Criteria.where("archived").ne(true));
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Capsule> capsules = mongoTemplate.find(query, Capsule.class);
        for (Capsule capsule : capsules) {
            long count = mongoTemplate.count(Query.query(Criteria.where("capsuleId").is(capsule.getId()))
                    .addCriteria(Criteria.where("archived").ne(true)), com.backend.rootly.entity.CapsuleEntry.class);
            capsule.setMemoryCount(count);
        }
        return capsules;
    }

    @Override
    public Capsule update(String id, CreateCapsuleRequestDTO request, UserReg user) {
        Capsule capsule = owned(id, user);
        capsule.requireOpen();
        request.validateUnlockCondition();
        capsule.setTitle(request.getTitle());
        capsule.setDescription(request.getDescription());
        capsule.setCoverPhotoUrl(request.getCoverPhotoUrl());
        capsule.setType(request.getType());
        capsule.setAllowContributions(request.getAllowContributions());
        capsule.setPrivacy(request.getPrivacy());
        capsule.setUnlockCondition(request.getUnlockCondition());
        capsule.setUpdatedAt(Instant.now());
        return mongoTemplate.save(capsule);
    }

    @Override
    public void delete(String id, UserReg user) {
        Capsule capsule = owned(id, user);
        // Reversible removal from the vault; memories remain attached to their owner.
        capsule.setArchived(true);
        capsule.setUpdatedAt(Instant.now());
        mongoTemplate.save(capsule);
    }

    private Capsule owned(String id, UserReg user) {
        String userId = userId(user);
        Capsule capsule = mongoTemplate.findById(id, Capsule.class);
        if (capsule == null || Boolean.TRUE.equals(capsule.getArchived()) || !userId.equals(capsule.getCreatorId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Capsule not found.");
        }
        return capsule;
    }

    private static String userId(UserReg user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in.");
        }
        return user.getId();
    }
}
