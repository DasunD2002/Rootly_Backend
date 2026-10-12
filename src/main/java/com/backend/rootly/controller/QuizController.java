package com.backend.rootly.controller;

import com.backend.rootly.dto.request.SubmitQuizAnswerRequestDTO;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.service.QuizService;
import com.backend.rootly.utility.EndPoint;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping(EndPoint.API)
@CrossOrigin
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping(value = EndPoint.QUIZ_DASHBOARD, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> dashboard(
            @AuthenticationPrincipal UserReg user,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        return quizService.dashboard(user, locale);
    }

    @PostMapping(value = EndPoint.QUIZ_SESSIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> startOrResume(
            @AuthenticationPrincipal UserReg user,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        return quizService.startOrResume(user, locale);
    }

    @PostMapping(value = EndPoint.QUIZ_ANSWERS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> submitAnswer(
            @PathVariable String sessionId,
            @Validated @RequestBody SubmitQuizAnswerRequestDTO request,
            @AuthenticationPrincipal UserReg user,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        return quizService.submitAnswer(sessionId, request, user, locale);
    }
}
