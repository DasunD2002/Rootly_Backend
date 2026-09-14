package com.backend.rootly.repository;

import com.backend.rootly.entity.CapsuleInvite;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CapsuleInviteRepository extends MongoRepository<CapsuleInvite, String> {

    Optional<CapsuleInvite> findByInviteToken(String inviteToken);

    List<CapsuleInvite> findByCapsuleId(String capsuleId);

    Optional<CapsuleInvite> findByCapsuleIdAndInviteeEmail(String capsuleId, String inviteeEmail);
}
