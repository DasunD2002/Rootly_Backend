package com.backend.rootly.service.impl;

import com.backend.rootly.dto.request.CapsuleEntryRequestDTO;
import com.backend.rootly.entity.Capsule;
import com.backend.rootly.entity.CapsuleEntry;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.enums.CapsuleStatus;
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
public class CapsuleMemoryService {
    private final MongoTemplate mongoTemplate;

    public List<CapsuleEntry> list(String capsuleId, UserReg user) {
        Capsule capsule = accessible(capsuleId, user);
        if (capsule.isLocked()) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "This capsule is sealed until its unlock date.");
        }
        return mongoTemplate.find(Query.query(Criteria.where("capsuleId").is(capsuleId))
                .addCriteria(Criteria.where("archived").ne(true))
                .with(Sort.by(Sort.Direction.ASC, "createdAt")), CapsuleEntry.class);
    }

    public CapsuleEntry add(String capsuleId, CapsuleEntryRequestDTO request, UserReg user) {
        Capsule capsule = accessible(capsuleId, user);
        requireOpen(capsule);
        if (!user.getId().equals(capsule.getCreatorId())
                && (!Boolean.TRUE.equals(capsule.getAllowContributions())
                || !contains(capsule.getContributorIds(), user.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contributions are not enabled for your account.");
        }
        Instant now = Instant.now();
        CapsuleEntry entry = CapsuleEntry.builder().capsuleId(capsuleId).contributorId(user.getId())
                .type(request.type()).content(request.content()).caption(request.caption())
                .createdAt(now).updatedAt(now).build();
        return mongoTemplate.save(entry);
    }

    public CapsuleEntry update(String capsuleId, String entryId, CapsuleEntryRequestDTO request, UserReg user) {
        requireOpen(accessible(capsuleId, user));
        CapsuleEntry entry = ownedEntry(capsuleId, entryId, user);
        entry.setType(request.type());
        entry.setContent(request.content());
        entry.setCaption(request.caption());
        entry.setUpdatedAt(Instant.now());
        return mongoTemplate.save(entry);
    }

    public void archive(String capsuleId, String entryId, UserReg user) {
        requireOpen(accessible(capsuleId, user));
        CapsuleEntry entry = ownedEntry(capsuleId, entryId, user);
        entry.setArchived(true);
        entry.setUpdatedAt(Instant.now());
        mongoTemplate.save(entry);
    }

    public Capsule seal(String capsuleId, UserReg user) {
        Capsule capsule = accessible(capsuleId, user);
        if (!user.getId().equals(capsule.getCreatorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can seal a capsule.");
        }
        capsule.validateSealing();
        capsule.setStatus(CapsuleStatus.SEALED);
        capsule.setSealedAt(Instant.now());
        capsule.setUpdatedAt(Instant.now());
        return mongoTemplate.save(capsule);
    }

    private Capsule accessible(String id, UserReg user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in.");
        }
        Capsule capsule = mongoTemplate.findById(id, Capsule.class);
        if (capsule == null || Boolean.TRUE.equals(capsule.getArchived()) || !isMember(capsule, user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Capsule not found.");
        }
        return capsule;
    }

    private CapsuleEntry ownedEntry(String capsuleId, String entryId, UserReg user) {
        CapsuleEntry entry = mongoTemplate.findById(entryId, CapsuleEntry.class);
        if (entry == null || Boolean.TRUE.equals(entry.getArchived()) || !capsuleId.equals(entry.getCapsuleId())
                || !user.getId().equals(entry.getContributorId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Memory not found.");
        }
        return entry;
    }

    private static boolean isMember(Capsule capsule, String id) {
        return id.equals(capsule.getCreatorId()) || contains(capsule.getContributorIds(), id)
                || contains(capsule.getSharedWithUserIds(), id);
    }

    private static boolean contains(List<String> ids, String id) {
        return ids != null && ids.contains(id);
    }

    private static void requireOpen(Capsule capsule) {
        capsule.requireOpen();
    }
}
