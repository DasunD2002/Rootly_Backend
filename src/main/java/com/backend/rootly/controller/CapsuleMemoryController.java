package com.backend.rootly.controller;

import com.backend.rootly.dto.request.CapsuleEntryRequestDTO;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.service.impl.CapsuleMemoryService;
import com.backend.rootly.utility.EndPoint;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping(EndPoint.API + "/v1/capsules/{capsuleId}")
@CrossOrigin
@RequiredArgsConstructor
public class CapsuleMemoryController {
    private static final String DATA = "data";
    private final CapsuleMemoryService service;

    @GetMapping("/entries")
    public ResponseEntity<Object> list(@PathVariable String capsuleId, @AuthenticationPrincipal UserReg user) {
        return ResponseEntity.ok(Map.of(DATA, service.list(capsuleId, user)));
    }

    @PostMapping("/entries")
    public ResponseEntity<Object> add(@PathVariable String capsuleId, @Valid @RequestBody CapsuleEntryRequestDTO request,
                                      @AuthenticationPrincipal UserReg user) {
        return ResponseEntity.status(201).body(Map.of(DATA, service.add(capsuleId, request, user)));
    }

    @PutMapping("/entries/{entryId}")
    public ResponseEntity<Object> update(@PathVariable String capsuleId, @PathVariable String entryId,
                                         @Valid @RequestBody CapsuleEntryRequestDTO request,
                                         @AuthenticationPrincipal UserReg user) {
        return ResponseEntity.ok(Map.of(DATA, service.update(capsuleId, entryId, request, user)));
    }

    @DeleteMapping("/entries/{entryId}")
    public ResponseEntity<Object> archive(@PathVariable String capsuleId, @PathVariable String entryId,
                                          @AuthenticationPrincipal UserReg user) {
        service.archive(capsuleId, entryId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/seal")
    public ResponseEntity<Object> seal(@PathVariable String capsuleId, @AuthenticationPrincipal UserReg user) {
        return ResponseEntity.ok(Map.of(DATA, service.seal(capsuleId, user)));
    }
}
