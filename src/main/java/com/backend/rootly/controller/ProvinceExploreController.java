package com.backend.rootly.controller;

import com.backend.rootly.domain.ProvinceExploreRequest;
import com.backend.rootly.dto.request.ProvinceExploreRequestDTO;
import com.backend.rootly.service.ProvinceExploreService;
import com.backend.rootly.utility.EndPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EndPoint.API)
@RequiredArgsConstructor
@Log4j2
public class ProvinceExploreController {
    private final ModelMapper modelMapper;
    private final ProvinceExploreService provinceExploreService;

    @PostMapping(value = EndPoint.EXPLORE_PROVINCE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> explore(@Validated @RequestBody ProvinceExploreRequestDTO requestDTO) {
        log.debug("Received Explore Province request");
        return provinceExploreService.explore(modelMapper.map(requestDTO, ProvinceExploreRequest.class));
    }
}
