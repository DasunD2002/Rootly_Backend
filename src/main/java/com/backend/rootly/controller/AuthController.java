package com.backend.rootly.controller;

import com.backend.rootly.domain.UserLogin;
import com.backend.rootly.domain.UserRegister;
import com.backend.rootly.dto.request.LoginRequestDTO;
import com.backend.rootly.dto.request.RegisterRequestDTO;
import com.backend.rootly.service.AuthService;
import com.backend.rootly.utility.EndPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@Log4j2
public class AuthController {
/*backend*/
    private final AuthService authService;
    private final ModelMapper modelMapper;

    @PostMapping(value = {EndPoint.AUTH_REGISTER, "/auth/register"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> register(
            @Validated @RequestBody RegisterRequestDTO requestDTO,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received User Register request");
        }
        UserRegister userRegister = modelMapper.map(requestDTO, UserRegister.class);
        return authService.register(userRegister, locale);
    }

    @PostMapping(value = {EndPoint.AUTH_LOGIN, "/auth/login"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> login(
            @Validated @RequestBody LoginRequestDTO requestDTO,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received User Login request");
        }
        UserLogin userLogin = modelMapper.map(requestDTO, UserLogin.class);
        return authService.authenticate(userLogin, locale);
    }
}
