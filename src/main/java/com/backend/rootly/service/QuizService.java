package com.backend.rootly.service;

import com.backend.rootly.dto.request.SubmitQuizAnswerRequestDTO;
import com.backend.rootly.entity.UserReg;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface QuizService {

    ResponseEntity<Object> dashboard(UserReg user, Locale locale);

    ResponseEntity<Object> startOrResume(UserReg user, Locale locale);

    ResponseEntity<Object> submitAnswer(String sessionId, SubmitQuizAnswerRequestDTO request,
                                        UserReg user, Locale locale);
}
