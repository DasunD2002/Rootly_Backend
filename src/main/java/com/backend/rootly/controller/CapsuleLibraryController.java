package com.backend.rootly.controller;

import com.backend.rootly.dto.request.CreateCapsuleRequestDTO;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.service.CapsuleLibraryService;
import com.backend.rootly.utility.EndPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping(EndPoint.API)
@CrossOrigin
@RequiredArgsConstructor
public class CapsuleLibraryController {
    private final CapsuleLibraryService capsuleLibraryService;

    @GetMapping(EndPoint.CAPSULES)
    public ResponseEntity<Object> list(@AuthenticationPrincipal UserReg user) {
        return ResponseEntity.ok(Map.of("data", capsuleLibraryService.list(user)));
    }

    @PutMapping(EndPoint.CAPSULE_DETAIL)
    public ResponseEntity<Object> update(@PathVariable String capsuleId,
                                        @Validated @RequestBody CreateCapsuleRequestDTO request,
                                        @AuthenticationPrincipal UserReg user) {
        return ResponseEntity.ok(Map.of("data", capsuleLibraryService.update(capsuleId, request, user)));
    }

    @DeleteMapping(EndPoint.CAPSULE_DETAIL)
    public ResponseEntity<Object> delete(@PathVariable String capsuleId, @AuthenticationPrincipal UserReg user) {
        capsuleLibraryService.delete(capsuleId, user);
        return ResponseEntity.noContent().build();
    }
}
