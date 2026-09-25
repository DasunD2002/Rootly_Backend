package com.backend.rootly.controller;

import com.backend.rootly.domain.ExplorePlacesRequest;
import com.backend.rootly.dto.request.ExplorePlacesRequestDTO;
import com.backend.rootly.service.ExplorePlacesService;
import com.backend.rootly.utility.EndPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EndPoint.API)
@RequiredArgsConstructor
@Log4j2
public class ExplorePlacesController {

    private final ModelMapper modelMapper;
    private final ExplorePlacesService explorePlacesService;

    @PostMapping(value = EndPoint.EXPLORE_PLACES, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> places(@Validated @RequestBody ExplorePlacesRequestDTO requestDTO) {
        if (log.isDebugEnabled()) {
            log.debug("Received Explore Places request");
        }
        return explorePlacesService.search(modelMapper.map(requestDTO, ExplorePlacesRequest.class));
    }

    @GetMapping(value = EndPoint.EXPLORE_PLACE_DETAIL, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> place(@PathVariable String placeId) {
        return explorePlacesService.getPlace(placeId);
    }

    @GetMapping(value = EndPoint.EXPLORE_CATEGORIES, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> categories() {
        log.debug("Received Explore Categories request");
        return ResponseEntity.ok(explorePlacesService.getCategories());
    }
}
