package com.backend.rootly.service;

import com.backend.rootly.domain.CreateCapsuleDomain;
import com.backend.rootly.domain.InviteContributorDomain;
import com.backend.rootly.dto.request.InviteRequestDTO;
import com.backend.rootly.dto.request.UpdateCapsuleRequestDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface CapsuleService {

    ResponseEntity<Object> createCapsule(CreateCapsuleDomain request, Locale locale);

    ResponseEntity<Object> getCapsuleDetail(String capsuleId, String requesterId, Locale locale);

    ResponseEntity<Object> updateCapsule(String capsuleId, String requesterId, UpdateCapsuleRequestDTO request, Locale locale);

    ResponseEntity<Object> deleteCapsule(String capsuleId, String requesterId, Locale locale);

    ResponseEntity<Object> inviteContributor(String capsuleId, InviteContributorDomain request, Locale locale);

    ResponseEntity<Object> createInvite(String capsuleId, String inviterId, InviteRequestDTO request, Locale locale);

    ResponseEntity<Object> joinByInvite(String inviteToken, String userId, Locale locale);
}
