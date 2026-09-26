package com.backend.rootly.service;

import com.backend.rootly.domain.ProvinceExploreRequest;
import org.springframework.http.ResponseEntity;

@FunctionalInterface
public interface ProvinceExploreService {
    ResponseEntity<Object> explore(ProvinceExploreRequest request);
}
