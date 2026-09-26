package com.backend.rootly.service.impl;

import com.backend.rootly.domain.UserLogin;
import com.backend.rootly.domain.UserRegister;
import com.backend.rootly.dto.ErrorResponse;
import com.backend.rootly.dto.ResponseDTO;
import com.backend.rootly.dto.response.LoginResponseDTO;
import com.backend.rootly.dto.response.RegisterResponseDTO;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.repository.UserRepository;
import com.backend.rootly.service.security.JwtService;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTests {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        MessageSource messageSource = mock(MessageSource.class);
        ModelMapper modelMapper = new ModelMapper();
        ResponseGenerator responseGenerator = new ResponseGenerator(modelMapper, messageSource);

        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtService, responseGenerator);
    }

    @Test
    void registerSuccessfullyCreatesUser() throws Exception {
        UserRegister request = UserRegister.builder()
                .name("John")
                .email("john@example.com")
                .password("secret123")
                .phone("0712345678")
                .gender("Male")
                .languages(List.of("English"))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("encodedPassword");

        UserReg saved = new UserReg();
        saved.setId("user-1");
        saved.setName("John");
        saved.setEmail("john@example.com");
        saved.setRole("USER");
        saved.setVerificationStatus("PENDING");

        when(userRepository.save(any(UserReg.class))).thenReturn(saved);

        ResponseEntity<Object> response = authService.register(request, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOf(ResponseDTO.class);
        ResponseDTO dto = (ResponseDTO) response.getBody();
        assertThat(dto.getResponseCode()).isEqualTo(ResponseCode.AUTH_REGISTER_SUCCESS);
        assertThat(dto.getData()).isInstanceOf(RegisterResponseDTO.class);
        RegisterResponseDTO regDto = (RegisterResponseDTO) dto.getData();
        assertThat(regDto.getId()).isEqualTo("user-1");
        verify(userRepository).save(any(UserReg.class));
    }

    @Test
    void registerWithExistingEmailReturnsConflict() throws Exception {
        UserRegister request = UserRegister.builder()
                .email("existing@example.com")
                .build();

        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(new UserReg()));

        ResponseEntity<Object> response = authService.register(request, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse err = (ErrorResponse) response.getBody();
        assertThat(err.getErrorCode()).isEqualTo(ResponseCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    void authenticateSuccessful() throws Exception {
        UserLogin request = UserLogin.builder()
                .email("john@example.com")
                .password("secret123")
                .build();

        UserReg user = new UserReg();
        user.setId("user-1");
        user.setName("John");
        user.setEmail("john@example.com");
        user.setPassword("encodedPassword");
        user.setRole("USER");
        user.setVerificationStatus("ACTIVE");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken("john@example.com")).thenReturn("jwt-token-123");

        ResponseEntity<Object> response = authService.authenticate(request, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ResponseDTO.class);
        ResponseDTO dto = (ResponseDTO) response.getBody();
        assertThat(dto.getResponseCode()).isEqualTo(ResponseCode.AUTH_LOGIN_SUCCESS);
        LoginResponseDTO loginDTO = (LoginResponseDTO) dto.getData();
        assertThat(loginDTO.getToken()).isEqualTo("jwt-token-123");
    }

    @Test
    void authenticateInvalidPasswordReturnsUnauthorized() throws Exception {
        UserLogin request = UserLogin.builder()
                .email("john@example.com")
                .password("wrongpassword")
                .build();

        UserReg user = new UserReg();
        user.setEmail("john@example.com");
        user.setPassword("encodedPassword");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        ResponseEntity<Object> response = authService.authenticate(request, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse err = (ErrorResponse) response.getBody();
        assertThat(err.getErrorCode()).isEqualTo(ResponseCode.INVALID_CREDENTIALS);
    }
}
