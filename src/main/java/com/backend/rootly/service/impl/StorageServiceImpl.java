package com.backend.rootly.service.impl;

import com.backend.rootly.dto.response.MediaUploadResponseDTO;
import com.backend.rootly.service.StorageService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Log4j2
public class StorageServiceImpl implements StorageService {

    private final Path rootLocation;
    private final String baseUrl;

    public StorageServiceImpl(
            @Value("${rootly.storage.upload-dir:uploads}") String uploadDir,
            @Value("${rootly.storage.base-url:http://localhost:8080/api/v1/media/files/}") String baseUrl) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            log.error("Could not initialize storage location: {}", e.getMessage(), e);
        }
    }

    @Override
    public MediaUploadResponseDTO store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        String originalFilename = file.getOriginalFilename();
        String cleanOriginalName = originalFilename != null ? Paths.get(originalFilename).getFileName().toString() : "file";
        String extension = "";
        int dotIdx = cleanOriginalName.lastIndexOf('.');
        if (dotIdx >= 0) {
            extension = cleanOriginalName.substring(dotIdx);
        }

        String storedFileName = UUID.randomUUID() + extension;
        Path destinationFile = this.rootLocation.resolve(storedFileName).normalize();

        if (!destinationFile.getParent().equals(this.rootLocation)) {
            throw new SecurityException("Cannot store file outside current directory");
        }

        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = baseUrl.endsWith("/") ? baseUrl + storedFileName : baseUrl + "/" + storedFileName;

        if (log.isInfoEnabled()) {
            log.info("File successfully stored: {} -> {}", cleanOriginalName, fileUrl);
        }

        return MediaUploadResponseDTO.builder()
                .url(fileUrl)
                .fileName(cleanOriginalName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .build();
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Could not read file: " + filename, e);
        }
    }
}
