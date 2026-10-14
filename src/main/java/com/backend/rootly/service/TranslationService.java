package com.backend.rootly.service;

import com.backend.rootly.domain.TranslationLookup;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface TranslationService {
    ResponseEntity<Object> lookup(TranslationLookup request, Locale locale);

    ResponseEntity<Object> glossary(String category, int page, int size, Locale locale);
}
