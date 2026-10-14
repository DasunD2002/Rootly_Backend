package com.backend.rootly.controller;

import com.backend.rootly.domain.TranslationLookup;
import com.backend.rootly.dto.request.TranslationLookupRequestDTO;
import com.backend.rootly.service.TranslationService;
import com.backend.rootly.utility.EndPoint;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping(EndPoint.API)
@CrossOrigin
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationService translationService;
    private final ModelMapper modelMapper;

    @PostMapping(value = EndPoint.TRANSLATION_LOOKUP,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> lookup(
            @Validated @RequestBody TranslationLookupRequestDTO requestDTO,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        return translationService.lookup(modelMapper.map(requestDTO, TranslationLookup.class), locale);
    }

    @GetMapping(value = EndPoint.TRANSLATION_GLOSSARY, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> glossary(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        return translationService.glossary(category, page, size, locale);
    }
}
