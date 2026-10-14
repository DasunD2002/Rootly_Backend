package com.backend.rootly.service;

import com.backend.rootly.domain.ExplorePlacesRequest;
import com.backend.rootly.dto.response.ExploreCategoryResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ExplorePlacesService {

    ResponseEntity<Object> search(ExplorePlacesRequest request);

    ResponseEntity<Object> getPlace(String placeId);

    List<ExploreCategoryResponseDTO> getCategories();
}
