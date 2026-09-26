package com.backend.rootly.service.impl;

import com.backend.rootly.domain.CreateCapsuleDomain;
import com.backend.rootly.domain.InviteContributorDomain;
import com.backend.rootly.domain.UpdateCapsuleDomain;
import com.backend.rootly.dto.response.CapsuleResponseDTO;
import com.backend.rootly.entity.Capsule;
import com.backend.rootly.enums.CapsulePrivacy;
import com.backend.rootly.enums.CapsuleStatus;
import com.backend.rootly.repository.CapsuleRepository;
import com.backend.rootly.service.CapsuleService;
import com.backend.rootly.utility.MessageConstant;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Log4j2
public class CapsuleServiceImpl implements CapsuleService {

    private final CapsuleRepository capsuleRepository;
    private final ResponseGenerator responseGenerator;
    private final ModelMapper modelMapper;
    private final Clock clock;

    @Autowired
    public CapsuleServiceImpl(CapsuleRepository capsuleRepository,
                              ResponseGenerator responseGenerator,
                              ModelMapper modelMapper,
                              @Autowired(required = false) Clock clock) {
        this.capsuleRepository = capsuleRepository;
        this.responseGenerator = responseGenerator;
        this.modelMapper = modelMapper;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Override
    @Transactional
    public ResponseEntity<Object> createCapsule(CreateCapsuleDomain request, Locale locale) {
        request.validateUnlockCondition(clock);

        List<String> sharedUserIds = cleanIds(request.getSharedWithUserIds());
        CapsulePrivacy privacy = request.getPrivacy();
        if (privacy == CapsulePrivacy.SHARED && sharedUserIds.isEmpty()) {
            return responseGenerator.generateErrorResponse(request, HttpStatus.BAD_REQUEST,
                    ResponseCode.BAD_REQUEST, "sharedWithUserIds is required when privacy is shared");
        }

        Instant now = Instant.now(clock);
        Capsule capsule = Capsule.builder()
                .creatorId(request.getCreatorId() != null ? request.getCreatorId().trim() : null)
                .title(trimToNull(request.getTitle()))
                .description(trimToNull(request.getDescription()))
                .coverPhotoUrl(trimToNull(request.getCoverPhotoUrl()))
                .type(request.getType())
                .unlockCondition(request.getUnlockCondition())
                .privacy(privacy)
                .sharedWithUserIds(sharedUserIds)
                .contributorIds(cleanIds(request.getContributorIds()))
                .status(CapsuleStatus.OPEN)
                .chainedFromCapsuleId(trimToNull(request.getChainedFromCapsuleId()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        Capsule savedCapsule = capsuleRepository.save(capsule);

        if (log.isInfoEnabled()) {
            log.info("Capsule created successfully with id: {}", savedCapsule.getId());
        }

        CapsuleResponseDTO responseDTO = modelMapper.map(savedCapsule, CapsuleResponseDTO.class);
        return responseGenerator.generateSuccessResponse(request, HttpStatus.CREATED,
                ResponseCode.CAPSULE_CREATE_SUCCESS, MessageConstant.CAPSULE_CREATE_SUCCESS, locale, responseDTO);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> inviteContributor(String capsuleId, InviteContributorDomain request, Locale locale) {
        String normalizedCapsuleId = requireId(capsuleId, "capsuleId is required");
        String contributorId = requireContributorId(request);

        Capsule capsule = capsuleRepository.findById(normalizedCapsuleId).orElse(null);
        if (capsule == null) {
            return responseGenerator.generateErrorResponse(request, HttpStatus.NOT_FOUND,
                    ResponseCode.CAPSULE_NOT_FOUND, MessageConstant.CAPSULE_NOT_FOUND, locale);
        }

        List<String> contributorIds = cleanIds(capsule.getContributorIds());
        capsule.validateForInvitation(contributorId, contributorIds);

        contributorIds.add(contributorId);
        capsule.setContributorIds(contributorIds);
        capsule.setUpdatedAt(Instant.now(clock));

        Capsule saved = capsuleRepository.save(capsule);

        if (log.isInfoEnabled()) {
            log.info("Contributor {} invited to capsule {}", contributorId, normalizedCapsuleId);
        }

        CapsuleResponseDTO responseDTO = modelMapper.map(saved, CapsuleResponseDTO.class);
        return responseGenerator.generateSuccessResponse(request, HttpStatus.OK,
                ResponseCode.CAPSULE_INVITE_SUCCESS, MessageConstant.CAPSULE_INVITE_SUCCESS, locale, responseDTO);
    }

    @Override
    public ResponseEntity<Object> getCapsule(String capsuleId, Locale locale) {
        String normalizedCapsuleId = requireId(capsuleId, "capsuleId is required");
        Capsule capsule = capsuleRepository.findById(normalizedCapsuleId).orElse(null);
        if (capsule == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.CAPSULE_NOT_FOUND, MessageConstant.CAPSULE_NOT_FOUND, locale);
        }

        CapsuleResponseDTO responseDTO = modelMapper.map(capsule, CapsuleResponseDTO.class);
        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.CAPSULE_GET_SUCCESS, MessageConstant.SUCCESSFULLY_GET, responseDTO);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> updateCapsule(String capsuleId, UpdateCapsuleDomain request, Locale locale) {
        String normalizedCapsuleId = requireId(capsuleId, "capsuleId is required");
        Capsule capsule = capsuleRepository.findById(normalizedCapsuleId).orElse(null);
        if (capsule == null) {
            return responseGenerator.generateErrorResponse(request, HttpStatus.NOT_FOUND,
                    ResponseCode.CAPSULE_NOT_FOUND, MessageConstant.CAPSULE_NOT_FOUND, locale);
        }

        if (request != null) {
            request.validateUnlockCondition(clock);

            if (request.getTitle() != null) {
                capsule.setTitle(trimToNull(request.getTitle()));
            }
            if (request.getDescription() != null) {
                capsule.setDescription(trimToNull(request.getDescription()));
            }
            if (request.getCoverPhotoUrl() != null) {
                capsule.setCoverPhotoUrl(trimToNull(request.getCoverPhotoUrl()));
            }
            if (request.getType() != null) {
                capsule.setType(request.getType());
            }
            if (request.getUnlockCondition() != null) {
                capsule.setUnlockCondition(request.getUnlockCondition());
            }
            if (request.getSharedWithUserIds() != null) {
                capsule.setSharedWithUserIds(cleanIds(request.getSharedWithUserIds()));
            }
            if (request.getPrivacy() != null) {
                if (request.getPrivacy() == CapsulePrivacy.SHARED) {
                    List<String> currentShared = capsule.getSharedWithUserIds();
                    if (currentShared == null || currentShared.isEmpty()) {
                        return responseGenerator.generateErrorResponse(request, HttpStatus.BAD_REQUEST,
                                ResponseCode.BAD_REQUEST, "sharedWithUserIds is required when privacy is shared");
                    }
                }
                capsule.setPrivacy(request.getPrivacy());
            }
            if (request.getStatus() != null) {
                capsule.setStatus(request.getStatus());
            }
        }

        capsule.setUpdatedAt(Instant.now(clock));
        Capsule saved = capsuleRepository.save(capsule);

        if (log.isInfoEnabled()) {
            log.info("Capsule updated successfully with id: {}", saved.getId());
        }

        CapsuleResponseDTO responseDTO = modelMapper.map(saved, CapsuleResponseDTO.class);
        return responseGenerator.generateSuccessResponse(request, HttpStatus.OK,
                ResponseCode.CAPSULE_UPDATE_SUCCESS, MessageConstant.CAPSULE_UPDATE_SUCCESS, locale, responseDTO);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> deleteCapsule(String capsuleId, Locale locale) {
        String normalizedCapsuleId = requireId(capsuleId, "capsuleId is required");
        Capsule capsule = capsuleRepository.findById(normalizedCapsuleId).orElse(null);
        if (capsule == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.CAPSULE_NOT_FOUND, MessageConstant.CAPSULE_NOT_FOUND, locale);
        }

        capsuleRepository.delete(capsule);

        if (log.isInfoEnabled()) {
            log.info("Capsule deleted successfully with id: {}", normalizedCapsuleId);
        }

        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.CAPSULE_DELETE_SUCCESS, MessageConstant.CAPSULE_DELETE_SUCCESS, null);
    }

    private static String requireContributorId(InviteContributorDomain request) {
        return requireId(request == null ? null : request.getContributorId(), "contributorId is required");
    }

    private static String requireId(String value, String message) {
        String id = trimToNull(value);
        if (id == null) {
            throw new IllegalArgumentException(message);
        }
        return id;
    }

    private static List<String> cleanIds(List<String> ids) {
        if (ids == null) {
            return new ArrayList<>();
        }
        Set<String> uniqueIds = new LinkedHashSet<>();
        for (String id : ids) {
            String cleanedId = trimToNull(id);
            if (cleanedId != null) {
                uniqueIds.add(cleanedId);
            }
        }
        return new ArrayList<>(uniqueIds);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
