package com.backend.rootly.controller;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.service.impl.UserFollowService;
import com.backend.rootly.utility.EndPoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping(EndPoint.API + "/v1/users/{userId}/follow")
@CrossOrigin
@RequiredArgsConstructor
public class UserFollowController {
    private final UserFollowService service;
    public record FollowRequest(@NotNull Boolean following) {}
    @GetMapping
    public ResponseEntity<Object> status(@PathVariable String userId, @AuthenticationPrincipal UserReg user) {
        return ResponseEntity.ok(Map.of("data", service.status(userId, user)));
    }
    @PutMapping
    public ResponseEntity<Object> update(@PathVariable String userId, @Valid @RequestBody FollowRequest request,
                                         @AuthenticationPrincipal UserReg user) {
        return ResponseEntity.ok(Map.of("data", service.update(userId, request.following(), user)));
    }
}
