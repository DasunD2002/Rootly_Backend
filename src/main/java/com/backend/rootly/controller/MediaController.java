package com.backend.rootly.controller;

import com.backend.rootly.dto.response.MediaUploadResponseDTO;
import com.backend.rootly.service.StorageService;
import com.backend.rootly.utility.EndPoint;
import com.backend.rootly.utility.MessageConstant;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping(EndPoint.API)
@CrossOrigin
@RequiredArgsConstructor
@Log4j2
public class MediaController {

    private final StorageService storageService;
    private final ResponseGenerator responseGenerator;

    @PostMapping(value = {
            EndPoint.MEDIA_UPLOAD,
            "/media/upload",
            "/v1/media/upload"
    }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> uploadMedia(@RequestParam("file") MultipartFile file) {
        if (log.isDebugEnabled()) {
            log.debug("Received media upload request: {}", file != null ? file.getOriginalFilename() : "null");
        }
        try {
            MediaUploadResponseDTO responseDTO = storageService.store(file);
            return responseGenerator.generateSuccessResponse(HttpStatus.CREATED,
                    ResponseCode.MEDIA_UPLOAD_SUCCESS, MessageConstant.MEDIA_UPLOAD_SUCCESS, responseDTO);
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Failed to store uploaded file: {}", e.getMessage(), e);
            }
            return responseGenerator.generateErrorResponse(null, HttpStatus.INTERNAL_SERVER_ERROR,
                    ResponseCode.INTERNAL_SERVER_ERROR, "Failed to upload file: " + e.getMessage());
        }
    }

    @GetMapping("/v1/media/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource file = storageService.loadAsResource(filename);
        String contentType = null;
        try {
            contentType = Files.probeContentType(Paths.get(file.getURI()));
        } catch (IOException e) {
            if (log.isWarnEnabled()) {
                log.warn("Could not determine file content type for {}", filename);
            }
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(file);
    }
}
