package com.backend.rootly.service;

import com.backend.rootly.domain.UserLogin;
import com.backend.rootly.domain.UserRegister;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface AuthService {

    ResponseEntity<Object> register(UserRegister request, Locale locale);

    ResponseEntity<Object> authenticate(UserLogin request, Locale locale);

    ResponseEntity<Object> googleAuth(String idToken, java.util.List<String> preferredLanguages, Locale locale);
}
