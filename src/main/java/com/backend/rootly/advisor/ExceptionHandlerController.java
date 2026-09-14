package com.backend.rootly.advisor;

import com.backend.rootly.dto.ErrorResponse;
import com.backend.rootly.exception.EmailAlreadyExistsException;
import com.backend.rootly.exception.InvalidCredentialsException;
import com.backend.rootly.exception.PlacesUnavailableException;
import com.backend.rootly.exception.ResourceNotFoundException;
import com.backend.rootly.exception.UserNotFoundException;
import com.backend.rootly.utility.MessageConstant;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
@Log4j2
@RequiredArgsConstructor
public class ExceptionHandlerController {

    private final ResponseGenerator responseGenerator;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        StringBuilder details = new StringBuilder();
        String firstMessage = "Validation error";

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            if (!details.isEmpty()) {
                details.append(", ");
            }
            details.append(fieldError.getField()).append(": ").append(fieldError.getDefaultMessage());
            if ("Validation error".equals(firstMessage) && fieldError.getDefaultMessage() != null) {
                firstMessage = fieldError.getDefaultMessage();
            }
        }

        log.error("Validation error: {}", details);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ResponseCode.REQUIRED_DATA_ELEMENT_MISSING)
                .errorDescription(firstMessage)
                .errorDetail(details.toString())
                .errorComponent("ROOTLY-BACKEND")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Object> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        if (log.isErrorEnabled()) {
            log.error("EmailAlreadyExistsException: {}", ex.getMessage());
        }
        Locale locale = LocaleContextHolder.getLocale();
        return responseGenerator.generateErrorResponse(null, HttpStatus.CONFLICT,
                ResponseCode.EMAIL_ALREADY_EXISTS, MessageConstant.EMAIL_ALREADY_EXISTS, locale);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Object> handleInvalidCredentials(InvalidCredentialsException ex) {
        if (log.isErrorEnabled()) {
            log.error("InvalidCredentialsException: {}", ex.getMessage());
        }
        Locale locale = LocaleContextHolder.getLocale();
        return responseGenerator.generateErrorResponse(null, HttpStatus.UNAUTHORIZED,
                ResponseCode.INVALID_CREDENTIALS, MessageConstant.INVALID_CREDENTIALS, locale);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Object> handleUserNotFound(UserNotFoundException ex) {
        if (log.isErrorEnabled()) {
            log.error("UserNotFoundException: {}", ex.getMessage());
        }
        Locale locale = LocaleContextHolder.getLocale();
        return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                ResponseCode.USER_NOT_FOUND, MessageConstant.USER_NOT_FOUND, locale);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex) {
        if (log.isErrorEnabled()) {
            log.error("ResourceNotFoundException: {}", ex.getMessage());
        }
        return responseGenerator.generateErrorResponse(HttpStatus.NOT_FOUND,
                ResponseCode.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PlacesUnavailableException.class)
    public ResponseEntity<Object> handlePlacesUnavailable(PlacesUnavailableException ex) {
        if (log.isErrorEnabled()) {
            log.error("PlacesUnavailableException: {}", ex.getMessage());
        }
        Locale locale = LocaleContextHolder.getLocale();
        ResponseEntity<Object> response = responseGenerator.generateErrorResponse(null, HttpStatus.SERVICE_UNAVAILABLE,
                ResponseCode.PLACES_UNAVAILABLE, MessageConstant.PLACES_UNAVAILABLE, locale);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(Math.max(1, ex.getRetryAfter().toSeconds())))
                .body(response.getBody());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex) {
        if (log.isErrorEnabled()) {
            log.error("IllegalArgumentException: {}", ex.getMessage());
        }
        return responseGenerator.generateErrorResponse(HttpStatus.BAD_REQUEST,
                ResponseCode.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalState(IllegalStateException ex) {
        if (log.isErrorEnabled()) {
            log.error("IllegalStateException: {}", ex.getMessage());
        }
        return responseGenerator.generateErrorResponse(HttpStatus.CONFLICT,
                ResponseCode.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        if (log.isWarnEnabled()) {
            log.warn("NoResourceFoundException for path '{}': {}", ex.getResourcePath(), ex.getMessage());
        }
        return responseGenerator.generateErrorResponse(HttpStatus.NOT_FOUND,
                ResponseCode.NOT_FOUND, "Resource or endpoint not found: '" + ex.getResourcePath() + "'. Please verify the URL and ensure there are no accidental trailing spaces or typos.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        log.error("Unhandled Exception: ", ex);
        Locale locale = LocaleContextHolder.getLocale();
        return responseGenerator.generateErrorResponse(null, HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseCode.INTERNAL_SERVER_ERROR, MessageConstant.SYSTEM_ERROR, locale);
    }
}
