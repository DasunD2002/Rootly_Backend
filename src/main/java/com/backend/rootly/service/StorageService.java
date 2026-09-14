package com.backend.rootly.service;

import com.backend.rootly.dto.response.MediaUploadResponseDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {

    MediaUploadResponseDTO store(MultipartFile file) throws IOException;

    Resource loadAsResource(String filename);
}
