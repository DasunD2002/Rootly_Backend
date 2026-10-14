package com.backend.rootly.service;

import org.springframework.http.ResponseEntity;

import java.util.Locale;

import com.backend.rootly.dto.request.UserUpdateRequestDTO;

public interface UserService {

    ResponseEntity<Object> getUserById(String userId, Locale locale);
    
    ResponseEntity<Object> updateUserProfile(String userId, UserUpdateRequestDTO request, Locale locale);

    ResponseEntity<Object> changePassword(String userId, com.backend.rootly.dto.request.ChangePasswordRequestDTO request, Locale locale);
}
