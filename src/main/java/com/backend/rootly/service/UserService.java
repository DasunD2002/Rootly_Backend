package com.backend.rootly.service;

import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface UserService {

    ResponseEntity<Object> getUserById(String userId, Locale locale);

    ResponseEntity<Object> getDashboardStats(String userId, Locale locale);
}
