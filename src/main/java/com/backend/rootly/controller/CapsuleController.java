package com.backend.rootly.controller;

import com.backend.rootly.domain.CreateCapsuleDomain;
import com.backend.rootly.domain.InviteContributorDomain;
import com.backend.rootly.domain.UpdateCapsuleDomain;
import com.backend.rootly.dto.request.CreateCapsuleRequestDTO;
import com.backend.rootly.dto.request.InviteContributorRequestDTO;
import com.backend.rootly.dto.request.UpdateCapsuleRequestDTO;
import com.backend.rootly.service.CapsuleService;
import com.backend.rootly.utility.EndPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private final CapsuleService capsuleService;
    private final ModelMapper modelMapper;

    @PostMapping(value = {EndPoint.CAPSULES, "/capsules"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createCapsule(
            @Validated @RequestBody CreateCapsuleRequestDTO requestDTO,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Create Capsule request");
        }
        CreateCapsuleDomain domain = modelMapper.map(requestDTO, CreateCapsuleDomain.class);
        return capsuleService.createCapsule(domain, locale);
    }

    @GetMapping(value = {EndPoint.CAPSULE_DETAIL, "/capsules/{capsuleId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getCapsule(
            @PathVariable String capsuleId,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Get Capsule request for id: {}", capsuleId);
        }
        return capsuleService.getCapsule(capsuleId, locale);
    }

    @PutMapping(value = {EndPoint.CAPSULE_DETAIL, "/capsules/{capsuleId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> updateCapsule(
            @PathVariable String capsuleId,
            @Validated @RequestBody UpdateCapsuleRequestDTO requestDTO,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Update Capsule request for id: {}", capsuleId);
        }
        UpdateCapsuleDomain domain = modelMapper.map(requestDTO, UpdateCapsuleDomain.class);
        return capsuleService.updateCapsule(capsuleId, domain, locale);
    }

    @DeleteMapping(value = {EndPoint.CAPSULE_DETAIL, "/capsules/{capsuleId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> deleteCapsule(
            @PathVariable String capsuleId,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Delete Capsule request for id: {}", capsuleId);
        }
        return capsuleService.deleteCapsule(capsuleId, locale);
    }

    @PostMapping(value = {
            EndPoint.CAPSULE_CONTRIBUTORS,
            EndPoint.CAPSULE_INVITE,
            "/capsules/{capsuleId}/contributors",
            "/capsules/{capsuleId}/invite"
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> inviteContributor(
            @PathVariable String capsuleId,
            @Validated @RequestBody InviteContributorRequestDTO requestDTO,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Invite Contributor request for capsule {}", capsuleId);
        }
        InviteContributorDomain domain = modelMapper.map(requestDTO, InviteContributorDomain.class);
        return capsuleService.inviteContributor(capsuleId, domain, locale);
    }
}
