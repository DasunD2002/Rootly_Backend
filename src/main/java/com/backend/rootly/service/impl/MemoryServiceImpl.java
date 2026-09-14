package com.backend.rootly.service.impl;

import com.backend.rootly.dto.request.CreateMemoryRequestDTO;
import com.backend.rootly.dto.response.MemoryResponseDTO;
import com.backend.rootly.entity.Capsule;
import com.backend.rootly.entity.CapsuleEntry;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.enums.NotificationType;
import com.backend.rootly.repository.CapsuleEntryRepository;
import com.backend.rootly.repository.CapsuleRepository;
import com.backend.rootly.repository.UserRepository;
import com.backend.rootly.service.MemoryService;
import com.backend.rootly.service.NotificationService;
import com.backend.rootly.utility.MessageConstant;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Log4j2
public class MemoryServiceImpl implements MemoryService {

    private final CapsuleRepository capsuleRepository;
    private final CapsuleEntryRepository capsuleEntryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ResponseGenerator responseGenerator;
    private final Clock clock;

    @Autowired
    public MemoryServiceImpl(CapsuleRepository capsuleRepository,
                             CapsuleEntryRepository capsuleEntryRepository,
                             UserRepository userRepository,
                             NotificationService notificationService,
                             ResponseGenerator responseGenerator,
                             @Autowired(required = false) Clock clock) {
        this.capsuleRepository = capsuleRepository;
        this.capsuleEntryRepository = capsuleEntryRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.responseGenerator = responseGenerator;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Override
    @Transactional
    public ResponseEntity<Object> createMemory(String capsuleId, String authorId,
                                              CreateMemoryRequestDTO request, Locale locale) {
        Capsule capsule = capsuleRepository.findById(capsuleId).orElse(null);
        if (capsule == null) {
            return responseGenerator.generateErrorResponse(request, HttpStatus.NOT_FOUND,
                    ResponseCode.CAPSULE_NOT_FOUND, MessageConstant.CAPSULE_NOT_FOUND, locale);
        }

        // Check if member contributions are allowed if requester is not creator
        boolean isCreator = authorId != null && authorId.equals(capsule.getCreatorId());
        boolean isContributor = capsule.getContributorIds() != null && capsule.getContributorIds().contains(authorId);
        boolean allowedContributions = capsule.getAllowMemberContributions() == null || capsule.getAllowMemberContributions();

        if (!isCreator && (!allowedContributions || !isContributor)) {
            return responseGenerator.generateErrorResponse(request, HttpStatus.FORBIDDEN,
                    ResponseCode.FORBIDDEN, "You do not have permission to add memories to this capsule", locale);
        }

        Instant now = Instant.now(clock);
        String legacyContent = request.getMediaUrl() != null && !request.getMediaUrl().isBlank()
                ? request.getMediaUrl() : request.getTextContent();

        CapsuleEntry entry = CapsuleEntry.builder()
                .capsuleId(capsuleId)
                .contributorId(authorId)
                .type(request.getType())
                .title(request.getTitle())
                .description(request.getDescription())
                .mediaUrl(request.getMediaUrl())
                .textContent(request.getTextContent())
                .content(legacyContent)
                .likesCount(0)
                .unlockDate(request.getUnlockDate())
                .createdAt(now)
                .updatedAt(now)
                .build();

        CapsuleEntry saved = capsuleEntryRepository.save(entry);

        // Notify creator if author is someone else
        if (!isCreator && capsule.getCreatorId() != null) {
            String authorName = "A contributor";
            if (authorId != null) {
                Optional<UserReg> authorOpt = userRepository.findById(authorId);
                if (authorOpt.isPresent()) {
                    authorName = authorOpt.get().getName();
                }
            }
            notificationService.createNotification(
                    capsule.getCreatorId(),
                    "Memory Added",
                    authorName + " added a new memory to '" + capsule.getTitle() + "'",
                    NotificationType.MEMORY_ADDED
            );
        }

        MemoryResponseDTO responseDTO = mapToMemoryDTO(saved, false);
        return responseGenerator.generateSuccessResponse(request, HttpStatus.CREATED,
                ResponseCode.MEMORY_CREATE_SUCCESS, MessageConstant.MEMORY_CREATE_SUCCESS, locale, responseDTO);
    }

    @Override
    public ResponseEntity<Object> getMemories(String capsuleId, String requesterId, Locale locale) {
        Capsule capsule = capsuleRepository.findById(capsuleId).orElse(null);
        if (capsule == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.CAPSULE_NOT_FOUND, MessageConstant.CAPSULE_NOT_FOUND, locale);
        }

        boolean isCapsuleLocked = isCapsuleCurrentlyLocked(capsule);
        List<CapsuleEntry> entries = capsuleEntryRepository.findByCapsuleIdOrderByCreatedAtDesc(capsuleId);

        List<MemoryResponseDTO> dtos = new ArrayList<>();
        for (CapsuleEntry entry : entries) {
            boolean isEntryLocked = isCapsuleLocked;
            if (!isEntryLocked && entry.getUnlockDate() != null) {
                isEntryLocked = Instant.now(clock).isBefore(entry.getUnlockDate());
            }
            dtos.add(mapToMemoryDTO(entry, isEntryLocked));
        }

        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.MEMORY_GET_SUCCESS, MessageConstant.MEMORY_GET_SUCCESS, dtos);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> reactToMemory(String capsuleId, String memoryId, String requesterId, Locale locale) {
        CapsuleEntry entry = capsuleEntryRepository.findById(memoryId).orElse(null);
        if (entry == null || !capsuleId.equals(entry.getCapsuleId())) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.MEMORY_NOT_FOUND, MessageConstant.MEMORY_NOT_FOUND, locale);
        }

        int currentLikes = entry.getLikesCount() != null ? entry.getLikesCount() : 0;
        entry.setLikesCount(currentLikes + 1);
        entry.setUpdatedAt(Instant.now(clock));
        capsuleEntryRepository.save(entry);

        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.MEMORY_REACT_SUCCESS, MessageConstant.MEMORY_REACT_SUCCESS, entry.getLikesCount());
    }

    public boolean isCapsuleCurrentlyLocked(Capsule capsule) {
        if (capsule.getStatus() != null && capsule.getStatus().isLocked()) {
            return true;
        }
        if (capsule.getUnlockCondition() != null && capsule.getUnlockCondition().getDate() != null) {
            return Instant.now(clock).isBefore(capsule.getUnlockCondition().getDate());
        }
        return false;
    }

    public MemoryResponseDTO mapToMemoryDTO(CapsuleEntry entry, boolean isLocked) {
        String authorName = null;
        String authorPhoto = null;
        if (entry.getContributorId() != null) {
            UserReg author = userRepository.findById(entry.getContributorId()).orElse(null);
            if (author != null) {
                authorName = author.getName();
                authorPhoto = author.getPhotoUrl();
            }
        }

        return MemoryResponseDTO.builder()
                .id(entry.getId())
                .capsuleId(entry.getCapsuleId())
                .authorId(entry.getContributorId())
                .authorName(authorName)
                .authorPhotoUrl(authorPhoto)
                .type(entry.getType())
                .title(entry.getTitle())
                .description(isLocked ? null : entry.getDescription())
                .mediaUrl(isLocked ? null : entry.getMediaUrl())
                .textContent(isLocked ? "[LOCKED - Content protected until unlock date]" : entry.getTextContent())
                .likesCount(entry.getLikesCount())
                .isLocked(isLocked)
                .unlockDate(entry.getUnlockDate())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
