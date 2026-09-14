package com.backend.rootly.service;

import com.backend.rootly.dto.request.CreateMemoryRequestDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface MemoryService {

    ResponseEntity<Object> createMemory(String capsuleId, String authorId, CreateMemoryRequestDTO request, Locale locale);

    ResponseEntity<Object> getMemories(String capsuleId, String requesterId, Locale locale);

    ResponseEntity<Object> reactToMemory(String capsuleId, String memoryId, String requesterId, Locale locale);
}
