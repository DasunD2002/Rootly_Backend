package com.backend.rootly.utility;

import com.backend.rootly.dto.ErrorResponse;
import com.backend.rootly.dto.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResponseGeneratorTests {

    private ResponseGenerator responseGenerator;
    private MessageSource messageSource;
    private ModelMapper modelMapper;

    @BeforeEach
    void setUp() {
        messageSource = mock(MessageSource.class);
        modelMapper = new ModelMapper();
        responseGenerator = new ResponseGenerator(modelMapper, messageSource);
    }

    @Test
    void generatesSuccessResponseWithCorrectPayload() {
        when(messageSource.getMessage(eq(MessageConstant.SUCCESSFULLY_GET), any(), any()))
                .thenReturn("OK");

        ResponseEntity<Object> response = responseGenerator.generateSuccessResponse(
                HttpStatus.OK, ResponseCode.RSP_SUCCESS, MessageConstant.SUCCESSFULLY_GET, "test-data");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ResponseDTO.class);

        ResponseDTO dto = (ResponseDTO) response.getBody();
        assertThat(dto.getResponseCode()).isEqualTo(ResponseCode.RSP_SUCCESS);
        assertThat(dto.getResponseDescription()).isEqualTo("OK");
        assertThat(dto.getData()).isEqualTo("test-data");
    }

    @Test
    void generatesErrorResponseWithCorrectPayload() {
        when(messageSource.getMessage(eq(MessageConstant.USER_NOT_FOUND), any(), any()))
                .thenReturn("User not found");

        ResponseEntity<Object> response = responseGenerator.generateErrorResponse(
                null, HttpStatus.NOT_FOUND, ResponseCode.USER_NOT_FOUND, MessageConstant.USER_NOT_FOUND, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);

        ErrorResponse err = (ErrorResponse) response.getBody();
        assertThat(err.getErrorCode()).isEqualTo(ResponseCode.USER_NOT_FOUND);
        assertThat(err.getErrorDescription()).isEqualTo("User not found");
    }
}
