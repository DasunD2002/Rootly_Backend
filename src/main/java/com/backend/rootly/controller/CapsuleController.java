package com.backend.rootly.controller;

import com.backend.rootly.domain.CreateCapsuleDomain;
import com.backend.rootly.domain.InviteContributorDomain;
import com.backend.rootly.dto.request.CreateCapsuleRequestDTO;
import com.backend.rootly.dto.request.InviteContributorRequestDTO;
import com.backend.rootly.dto.request.InviteRequestDTO;
import com.backend.rootly.dto.request.UpdateCapsuleRequestDTO;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.service.CapsuleService;
import com.backend.rootly.utility.EndPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping(EndPoint.API)
@CrossOrigin
@RequiredArgsConstructor
@Log4j2
public class CapsuleController {

    private static final String ACCEPT_LANGUAGE = "Accept-Language";

    private final CapsuleService capsuleService;
    private final ModelMapper modelMapper;

    @PostMapping(value = {EndPoint.CAPSULES, "/capsules"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createCapsule(
            @AuthenticationPrincipal UserReg user,
            @Validated @RequestBody CreateCapsuleRequestDTO requestDTO,
            @RequestHeader(value = ACCEPT_LANGUAGE, required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Create Capsule request");
        }
        if (requestDTO.getCreatorId() == null && user != null) {
            requestDTO.setCreatorId(user.getId());
        }
        CreateCapsuleDomain domain = modelMapper.map(requestDTO, CreateCapsuleDomain.class);
        return capsuleService.createCapsule(domain, locale);
    }

    @GetMapping(value = {
            EndPoint.CAPSULE_DETAIL,
            "/capsules/{capsuleId}",
            "/v1/capsules/{capsuleId}"
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getCapsuleDetail(
            @PathVariable String capsuleId,
            @AuthenticationPrincipal UserReg user,
            @RequestHeader(value = ACCEPT_LANGUAGE, required = false) Locale locale) {
        String requesterId = user != null ? user.getId() : null;
        if (log.isDebugEnabled()) {
            log.debug("Received Get Capsule Detail request for id: {}", capsuleId);
        }
        return capsuleService.getCapsuleDetail(capsuleId, requesterId, locale);
    }

    @PutMapping(value = {
            EndPoint.CAPSULE_DETAIL,
            "/capsules/{capsuleId}",
            "/v1/capsules/{capsuleId}"
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> updateCapsule(
            @PathVariable String capsuleId,
            @AuthenticationPrincipal UserReg user,
            @RequestBody UpdateCapsuleRequestDTO requestDTO,
            @RequestHeader(value = ACCEPT_LANGUAGE, required = false) Locale locale) {
        String requesterId = user != null ? user.getId() : null;
        if (log.isDebugEnabled()) {
            log.debug("Received Update Capsule request for id: {}", capsuleId);
        }
        return capsuleService.updateCapsule(capsuleId, requesterId, requestDTO, locale);
    }
    //delete

    @DeleteMapping(value = {
            EndPoint.CAPSULE_DETAIL,
            "/capsules/{capsuleId}",
            "/v1/capsules/{capsuleId}"
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> deleteCapsule(
            @PathVariable String capsuleId,
            @AuthenticationPrincipal UserReg user,
            @RequestHeader(value = ACCEPT_LANGUAGE, required = false) Locale locale) {
        String requesterId = user != null ? user.getId() : null;
        if (log.isDebugEnabled()) {
            log.debug("Received Delete Capsule request for id: {}", capsuleId);
        }
        return capsuleService.deleteCapsule(capsuleId, requesterId, locale);
    }

    @PostMapping(value = {
            EndPoint.CAPSULE_CONTRIBUTORS,
            EndPoint.CAPSULE_INVITE,
            "/capsules/{capsuleId}/contributors",
            "/capsules/{capsuleId}/invite",
            "/v1/capsules/{capsuleId}/invite"
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> inviteContributor(
            @PathVariable String capsuleId,
            @AuthenticationPrincipal UserReg user,
            @RequestBody InviteRequestDTO requestDTO,
            @RequestHeader(value = ACCEPT_LANGUAGE, required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Invite Contributor request for capsule {}", capsuleId);
        }
        String inviterId = user != null ? user.getId() : null;
        return capsuleService.createInvite(capsuleId, inviterId, requestDTO, locale);
    }

    @PostMapping(value = {
            "/v1/capsules/join/{inviteToken}",
            "/capsules/join/{inviteToken}"
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> joinByInvite(
            @PathVariable String inviteToken,
            @AuthenticationPrincipal UserReg user,
            @RequestHeader(value = ACCEPT_LANGUAGE, required = false) Locale locale) {
        String userId = user != null ? user.getId() : null;
        if (log.isDebugEnabled()) {
            log.debug("Received Join Capsule request with token: {}", inviteToken);
        }
        return capsuleService.joinByInvite(inviteToken, userId, locale);
    }
}
