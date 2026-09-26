package com.backend.rootly.advisor;

import com.backend.rootly.controller.TranslationController;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = TranslationController.class)
@RequiredArgsConstructor
public class TranslationApiExceptionHandler {

    private final ResponseGenerator responseGenerator;

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> unreadableBody(HttpMessageNotReadableException exception) {
        return responseGenerator.generateErrorResponse(HttpStatus.BAD_REQUEST,
                ResponseCode.BAD_REQUEST, "Request body must contain valid JSON fields");
    }
}
