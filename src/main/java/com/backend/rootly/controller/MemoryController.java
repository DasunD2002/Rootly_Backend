package com.backend.rootly.controller;

import com.backend.rootly.dto.request.CreateMemoryRequestDTO;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.service.MemoryService;
import com.backend.rootly.utility.EndPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping(EndPoint.API)
@CrossOrigin
@RequiredArgsConstructor
@Log4j2
public class MemoryController {

    private final MemoryService memoryService;

    @PostMapping(value = {
            EndPoint.CAPSULE_MEMORIES,
            "/capsules/{id}/memories",
            "/v1/capsules/{id}/memories"
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createMemory(
            @PathVariable String id,
            @AuthenticationPrincipal UserReg user,
            @RequestParam(value = "authorId", required = false) String queryAuthorId,
            @Validated @RequestBody CreateMemoryRequestDTO requestDTO,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        String effectiveAuthorId = user != null ? user.getId() : queryAuthorId;
        if (log.isDebugEnabled()) {
            log.debug("Received Add Memory request for capsule {} by author {}", id, effectiveAuthorId);
        }
        return memoryService.createMemory(id, effectiveAuthorId, requestDTO, locale);
    }

    @GetMapping(value = {
            EndPoint.CAPSULE_MEMORIES,
            "/capsules/{id}/memories",
            "/v1/capsules/{id}/memories"
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getMemories(
            @PathVariable String id,
            @AuthenticationPrincipal UserReg user,
            @RequestParam(value = "requesterId", required = false) String queryRequesterId,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        String requesterId = user != null ? user.getId() : queryRequesterId;
        if (log.isDebugEnabled()) {
            log.debug("Received Get Memories request for capsule {}", id);
        }
        return memoryService.getMemories(id, requesterId, locale);
    }

    @PostMapping(value = {
            EndPoint.CAPSULE_MEMORY_REACT,
            "/capsules/{id}/memories/{memoryId}/react",
            "/v1/capsules/{id}/memories/{memoryId}/react"
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> reactToMemory(
            @PathVariable String id,
            @PathVariable String memoryId,
            @AuthenticationPrincipal UserReg user,
            @RequestParam(value = "requesterId", required = false) String queryRequesterId,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        String requesterId = user != null ? user.getId() : queryRequesterId;
        if (log.isDebugEnabled()) {
            log.debug("Received React to Memory request for memory {}", memoryId);
        }
        return memoryService.reactToMemory(id, memoryId, requesterId, locale);
    }
}
