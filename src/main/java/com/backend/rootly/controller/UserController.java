package com.backend.rootly.controller;

import com.backend.rootly.entity.UserReg;
import com.backend.rootly.service.UserService;
import com.backend.rootly.utility.EndPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
public class UserController {

    private final UserService userService;

    @GetMapping(value = {EndPoint.USER_PROFILE, "/users/{userId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getUserProfile(
            @PathVariable String userId,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Get User Profile request for id: {}", userId);
        }
        return userService.getUserById(userId, locale);
    }

    @GetMapping(value = {EndPoint.USER_DASHBOARD_STATS, "/user/dashboard-stats"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getDashboardStats(
            @AuthenticationPrincipal UserReg user,
            @RequestParam(value = "userId", required = false) String queryUserId,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        String effectiveUserId = user != null ? user.getId() : queryUserId;
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (log.isDebugEnabled()) {
            log.debug("Received Get Dashboard Stats request for user: {}", effectiveUserId);
        }
        return userService.getDashboardStats(effectiveUserId, locale);
    }
}
