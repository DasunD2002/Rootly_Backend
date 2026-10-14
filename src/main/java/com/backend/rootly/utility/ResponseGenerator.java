package com.backend.rootly.utility;

import com.backend.rootly.dto.ErrorResponse;
import com.backend.rootly.dto.ResponseDTO;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.MappingException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@Log4j2
@SuppressWarnings("PMD.TooManyMethods")
public class ResponseGenerator {

    private final ModelMapper modelMapper;
    private final MessageSource messageSource;

    @Value("${error.component.name:ROOTLY-BACKEND}")
    private String errorComponent;

    @Autowired
    public ResponseGenerator(ModelMapper modelMapper, MessageSource messageSource) {
        this.modelMapper = modelMapper;
        this.messageSource = messageSource;
    }

    public ResponseEntity<Object> generateSuccessResponse(Object requestBean, HttpStatus httpStatus,
                                                          String responseCode, String responseDescription,
                                                          Object dataObject) {
        ResponseDTO responseDTO = mapOrCreateResponseDTO(requestBean);
        responseDTO.setResponseCode(responseCode);
        responseDTO.setResponseDescription(resolveMessage(responseDescription, null, null));
        responseDTO.setData(dataObject);
        return ResponseEntity.status(httpStatus).body(responseDTO);
    }

    public ResponseEntity<Object> generateSuccessResponse(Object requestBean, HttpStatus httpStatus,
                                                          String responseCode, String responseDescription,
                                                          Locale language, Object dataObject) {
        ResponseDTO responseDTO = mapOrCreateResponseDTO(requestBean);
        responseDTO.setResponseCode(responseCode);
        responseDTO.setResponseDescription(resolveMessage(responseDescription, null, language));
        responseDTO.setData(dataObject);
        return ResponseEntity.status(httpStatus).body(responseDTO);
    }

    public ResponseEntity<Object> generateSuccessResponse(Object requestBean, HttpStatus httpStatus,
                                                          String responseCode, String responseDescription,
                                                          Locale language, Object dataObject, Long fullCount) {
        ResponseDTO responseDTO = mapOrCreateResponseDTO(requestBean);
        responseDTO.setResponseCode(responseCode);
        responseDTO.setResponseDescription(resolveMessage(responseDescription, null, language));
        responseDTO.setData(dataObject);
        responseDTO.setFullCount(fullCount);
        return ResponseEntity.status(httpStatus).body(responseDTO);
    }

    public ResponseEntity<Object> generateSuccessResponse(HttpStatus httpStatus, String responseCode,
                                                          String messagePropertyName, Object dataObject) {
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setResponseCode(responseCode);
        responseDTO.setResponseDescription(resolveMessage(messagePropertyName, null, null));
        responseDTO.setData(dataObject);
        return ResponseEntity.status(httpStatus).body(responseDTO);
    }

    public ResponseEntity<Object> generateSuccessResponse(HttpStatus httpStatus, String responseCode,
                                                          String messagePropertyName, Object[] params, Locale lang) {
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setResponseCode(responseCode);
        responseDTO.setResponseDescription(resolveMessage(messagePropertyName, params, lang));
        return ResponseEntity.status(httpStatus).body(responseDTO);
    }

    public ResponseEntity<Object> generateErrorResponse(Object requestBean, HttpStatus httpStatus,
                                                        String errorCode, String errorDescriptionPropertyName,
                                                        Locale language) {
        ErrorResponse errorResponse = mapOrCreateErrorResponse(requestBean);
        errorResponse.setErrorCode(errorCode);
        errorResponse.setErrorDescription(resolveMessage(errorDescriptionPropertyName, null, language));
        errorResponse.setErrorComponent(errorComponent);
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    public ResponseEntity<Object> generateErrorResponse(Object requestBean, HttpStatus httpStatus,
                                                        String errorCode, String errorMessage) {
        ErrorResponse errorResponse = mapOrCreateErrorResponse(requestBean);
        errorResponse.setErrorCode(errorCode);
        errorResponse.setErrorDescription(resolveMessage(errorMessage, null, null));
        errorResponse.setErrorComponent(errorComponent);
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    public ResponseEntity<Object> generateErrorResponse(Object requestBean, HttpStatus httpStatus,
                                                        String errorCode, String errorMessage,
                                                        Locale language, Object dataObject) {
        ErrorResponse errorResponse = mapOrCreateErrorResponse(requestBean);
        errorResponse.setErrorCode(errorCode);
        errorResponse.setErrorDescription(resolveMessage(errorMessage, null, language));
        errorResponse.setData(dataObject);
        errorResponse.setErrorComponent(errorComponent);
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    public ResponseEntity<Object> generateErrorResponse(String errorCode, String errorDescription, Locale locale) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorCode(errorCode);
        errorResponse.setErrorDescription(resolveMessage(errorDescription, null, locale));
        errorResponse.setErrorComponent(errorComponent);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    public ResponseEntity<Object> generateErrorResponse(HttpStatus httpStatus, String errorCode, String errorMessage) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorCode(errorCode);
        errorResponse.setErrorDescription(resolveMessage(errorMessage, null, null));
        errorResponse.setErrorComponent(errorComponent);
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    public ResponseEntity<Object> generateNoContentResponse() {
        return ResponseEntity.noContent().build();
    }

    private ResponseDTO mapOrCreateResponseDTO(Object requestBean) {
        if (requestBean != null) {
            try {
                return modelMapper.map(requestBean, ResponseDTO.class);
            } catch (MappingException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Could not map requestBean to ResponseDTO: {}", e.getMessage());
                }
            }
        }
        return new ResponseDTO();
    }

    private ErrorResponse mapOrCreateErrorResponse(Object requestBean) {
        if (requestBean != null) {
            try {
                return modelMapper.map(requestBean, ErrorResponse.class);
            } catch (MappingException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Could not map requestBean to ErrorResponse: {}", e.getMessage());
                }
            }
        }
        return new ErrorResponse();
    }

    private String resolveMessage(String propertyOrMessage, Object[] params, Locale locale) {
        if (propertyOrMessage == null) {
            return "";
        }
        try {
            Locale targetLocale = locale != null ? locale : LocaleContextHolder.getLocale();
            return messageSource.getMessage(propertyOrMessage, params, targetLocale);
        } catch (NoSuchMessageException e) {
            return propertyOrMessage;
        }
    }
}
