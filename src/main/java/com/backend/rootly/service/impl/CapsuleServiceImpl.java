package com.backend.rootly.service.impl;

import com.backend.rootly.domain.CreateCapsuleDomain;
import com.backend.rootly.domain.InviteContributorDomain;
import com.backend.rootly.dto.request.InviteRequestDTO;
import com.backend.rootly.dto.request.UpdateCapsuleRequestDTO;
import com.backend.rootly.dto.response.CapsuleDetailResponseDTO;
import com.backend.rootly.dto.response.CapsuleResponseDTO;
import com.backend.rootly.dto.response.ContributorDTO;
import com.backend.rootly.dto.response.InviteResponseDTO;
import com.backend.rootly.dto.response.MemoryResponseDTO;
import com.backend.rootly.entity.Capsule;
import com.backend.rootly.entity.CapsuleEntry;
import com.backend.rootly.entity.CapsuleInvite;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.enums.CapsulePrivacy;
import com.backend.rootly.enums.CapsuleStatus;
import com.backend.rootly.enums.InviteStatus;
import com.backend.rootly.enums.NotificationType;
import com.backend.rootly.repository.CapsuleEntryRepository;
import com.backend.rootly.repository.CapsuleInviteRepository;
import com.backend.rootly.repository.CapsuleRepository;
import com.backend.rootly.repository.UserRepository;
import com.backend.rootly.service.CapsuleService;
import com.backend.rootly.service.NotificationService;
import com.backend.rootly.utility.MessageConstant;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import com.backend.rootly.utility.TimeUtil;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Log4j2
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports", "PMD.CouplingBetweenObjects"})
public class CapsuleServiceImpl implements CapsuleService {

    private static final String CAPSULE_ID_REQUIRED = "capsuleId is required";

    private final CapsuleRepository capsuleRepository;
    private final CapsuleEntryRepository capsuleEntryRepository;
    private final UserRepository userRepository;
    private final CapsuleInviteRepository capsuleInviteRepository;
    private final NotificationService notificationService;
    private final ResponseGenerator responseGenerator;
    private final ModelMapper modelMapper;
    private final Clock clock;

    @Autowired
    public CapsuleServiceImpl(CapsuleRepository capsuleRepository,
                              CapsuleEntryRepository capsuleEntryRepository,
                              UserRepository userRepository,
                              CapsuleInviteRepository capsuleInviteRepository,
                              NotificationService notificationService,
                              ResponseGenerator responseGenerator,
                              ModelMapper modelMapper,
                              @Autowired(required = false) Clock clock) {
        this.capsuleRepository = capsuleRepository;
        this.capsuleEntryRepository = capsuleEntryRepository;
        this.userRepository = userRepository;
        this.capsuleInviteRepository = capsuleInviteRepository;
        this.notificationService = notificationService;
        this.responseGenerator = responseGenerator;
        this.modelMapper = modelMapper;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Override
    @Transactional
    public ResponseEntity<Object> createCapsule(CreateCapsuleDomain request, Locale locale) {
        request.validateUnlockCondition(clock);

        if (request.getCreatorId() == null || request.getCreatorId().isBlank()) {
            return responseGenerator.generateErrorResponse(request, HttpStatus.BAD_REQUEST,
                    ResponseCode.BAD_REQUEST, "creatorId is required");
        }

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
                .allowMemberContributions(true)
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
    @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity", "PMD.NPathComplexity", "PMD.LawOfDemeter", "PMD.AvoidDeeplyNestedIfStmts"})
    public ResponseEntity<Object> getCapsuleDetail(String capsuleId, String requesterId, Locale locale) {
        String normalizedCapsuleId = requireId(capsuleId, CAPSULE_ID_REQUIRED);
        Capsule capsule = capsuleRepository.findById(normalizedCapsuleId).orElse(null);
        if (capsule == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.CAPSULE_NOT_FOUND, MessageConstant.CAPSULE_NOT_FOUND, locale);
        }

        Instant now = Instant.now(clock);
        Instant unlockDate = capsule.getUnlockCondition() != null ? capsule.getUnlockCondition().getDate() : null;
        boolean isLocked = (capsule.getStatus() != null && capsule.getStatus().isLocked())
                || (unlockDate != null && now.isBefore(unlockDate));

        String countdown = TimeUtil.formatCountdown(unlockDate, now);

        // Contributors list
        List<ContributorDTO> contributors = new ArrayList<>();
        if (capsule.getCreatorId() != null) {
            UserReg creator = userRepository.findById(capsule.getCreatorId()).orElse(null);
            if (creator != null) {
                contributors.add(ContributorDTO.builder()
                        .id(creator.getId())
                        .name(creator.getName())
                        .email(creator.getEmail())
                        .photoUrl(creator.getPhotoUrl())
                        .role("Creator")
                        .build());
            }
        }

        if (capsule.getContributorIds() != null) {
            for (String contribId : capsule.getContributorIds()) {
                if (contribId != null && !contribId.equals(capsule.getCreatorId())) {
                    UserReg contributor = userRepository.findById(contribId).orElse(null);
                    if (contributor != null) {
                        contributors.add(ContributorDTO.builder()
                                .id(contributor.getId())
                                .name(contributor.getName())
                                .email(contributor.getEmail())
                                .photoUrl(contributor.getPhotoUrl())
                                .role("Contributor")
                                .build());
                    }
                }
            }
        }

        // Memories with payload protection
        List<CapsuleEntry> entries = capsuleEntryRepository.findByCapsuleIdOrderByCreatedAtDesc(normalizedCapsuleId);
        List<MemoryResponseDTO> memoryDTOs = new ArrayList<>();
        for (CapsuleEntry entry : entries) {
            boolean entryLocked = isLocked;
            if (!entryLocked && entry.getUnlockDate() != null) {
                entryLocked = now.isBefore(entry.getUnlockDate());
            }

            String authorName = null;
            String authorPhoto = null;
            if (entry.getContributorId() != null) {
                UserReg author = userRepository.findById(entry.getContributorId()).orElse(null);
                if (author != null) {
                    authorName = author.getName();
                    authorPhoto = author.getPhotoUrl();
                }
            }

            memoryDTOs.add(MemoryResponseDTO.builder()
                    .id(entry.getId())
                    .capsuleId(entry.getCapsuleId())
                    .authorId(entry.getContributorId())
                    .authorName(authorName)
                    .authorPhotoUrl(authorPhoto)
                    .type(entry.getType())
                    .title(entry.getTitle())
                    .description(entryLocked ? null : entry.getDescription())
                    .mediaUrl(entryLocked ? null : entry.getMediaUrl())
                    .textContent(entryLocked ? "[LOCKED - Content protected until unlock date]" : entry.getTextContent())
                    .likesCount(entry.getLikesCount())
                    .isLocked(entryLocked)
                    .unlockDate(entry.getUnlockDate())
                    .createdAt(entry.getCreatedAt())
                    .build());
        }

        String creatorName = null;
        if (capsule.getCreatorId() != null) {
            UserReg creator = userRepository.findById(capsule.getCreatorId()).orElse(null);
            if (creator != null) {
                creatorName = creator.getName();
            }
        }

        CapsuleDetailResponseDTO detailDTO = CapsuleDetailResponseDTO.builder()
                .id(capsule.getId())
                .creatorId(capsule.getCreatorId())
                .creatorName(creatorName)
                .title(capsule.getTitle())
                .description(capsule.getDescription())
                .coverPhotoUrl(capsule.getCoverPhotoUrl())
                .category(capsule.getType())
                .status(isLocked ? CapsuleStatus.LOCKED : capsule.getStatus())
                .unlockCondition(capsule.getUnlockCondition())
                .unlockDate(unlockDate)
                .countdown(countdown)
                .privacy(capsule.getPrivacy())
                .allowMemberContributions(capsule.getAllowMemberContributions())
                .contributors(contributors)
                .memories(memoryDTOs)
                .createdAt(capsule.getCreatedAt())
                .updatedAt(capsule.getUpdatedAt())
                .build();

        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.CAPSULE_GET_SUCCESS, MessageConstant.SUCCESSFULLY_GET, detailDTO);
    }

    @Override
    @Transactional
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity", "PMD.LawOfDemeter"})
    public ResponseEntity<Object> updateCapsule(String capsuleId, String requesterId,
                                               UpdateCapsuleRequestDTO request, Locale locale) {
        String normalizedCapsuleId = requireId(capsuleId, CAPSULE_ID_REQUIRED);
        Capsule capsule = capsuleRepository.findById(normalizedCapsuleId).orElse(null);
        if (capsule == null) {
            return responseGenerator.generateErrorResponse(request, HttpStatus.NOT_FOUND,
                    ResponseCode.CAPSULE_NOT_FOUND, MessageConstant.CAPSULE_NOT_FOUND, locale);
        }

        if (requesterId != null && !requesterId.equals(capsule.getCreatorId())) {
            return responseGenerator.generateErrorResponse(request, HttpStatus.FORBIDDEN,
                    ResponseCode.FORBIDDEN, "Only the capsule creator can modify settings", locale);
        }

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
        if (request.getStatus() != null) {
            capsule.setStatus(request.getStatus());
        }
        if (request.getPrivacy() != null) {
            capsule.setPrivacy(request.getPrivacy());
        }
        if (request.getUnlockCondition() != null) {
            request.getUnlockCondition().validate(clock);
            capsule.setUnlockCondition(request.getUnlockCondition());
        }
        if (request.getAllowMemberContributions() != null) {
            capsule.setAllowMemberContributions(request.getAllowMemberContributions());
        }
        if (request.getSharedWithUserIds() != null) {
            capsule.setSharedWithUserIds(cleanIds(request.getSharedWithUserIds()));
        }

        capsule.setUpdatedAt(Instant.now(clock));
        Capsule saved = capsuleRepository.save(capsule);

        CapsuleResponseDTO responseDTO = modelMapper.map(saved, CapsuleResponseDTO.class);
        return responseGenerator.generateSuccessResponse(request, HttpStatus.OK,
                ResponseCode.CAPSULE_UPDATE_SUCCESS, MessageConstant.CAPSULE_UPDATE_SUCCESS, locale, responseDTO);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> deleteCapsule(String capsuleId, String requesterId, Locale locale) {
        String normalizedCapsuleId = requireId(capsuleId, CAPSULE_ID_REQUIRED);
        Capsule capsule = capsuleRepository.findById(normalizedCapsuleId).orElse(null);
        if (capsule == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.CAPSULE_NOT_FOUND, MessageConstant.CAPSULE_NOT_FOUND, locale);
        }

        if (requesterId != null && !requesterId.equals(capsule.getCreatorId())) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.FORBIDDEN,
                    ResponseCode.FORBIDDEN, "Only the capsule creator can delete this capsule", locale);
        }

        capsuleEntryRepository.deleteByCapsuleId(normalizedCapsuleId);
        capsuleRepository.delete(capsule);

        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.CAPSULE_DELETE_SUCCESS, MessageConstant.CAPSULE_DELETE_SUCCESS, null);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> inviteContributor(String capsuleId, InviteContributorDomain request, Locale locale) {
        String normalizedCapsuleId = requireId(capsuleId, CAPSULE_ID_REQUIRED);
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

        notificationService.createNotification(
                contributorId,
                "Capsule Invite",
                "You have been invited to collaborate on capsule '" + capsule.getTitle() + "'",
                NotificationType.CAPSULE_INVITE
        );

        CapsuleResponseDTO responseDTO = modelMapper.map(saved, CapsuleResponseDTO.class);
        return responseGenerator.generateSuccessResponse(request, HttpStatus.OK,
                ResponseCode.CAPSULE_INVITE_SUCCESS, MessageConstant.CAPSULE_INVITE_SUCCESS, locale, responseDTO);
    }

    @Override
    @Transactional
    @SuppressWarnings("PMD.LawOfDemeter")
    public ResponseEntity<Object> createInvite(String capsuleId, String inviterId,
                                              InviteRequestDTO request, Locale locale) {
        String normalizedCapsuleId = requireId(capsuleId, CAPSULE_ID_REQUIRED);
        Capsule capsule = capsuleRepository.findById(normalizedCapsuleId).orElse(null);
        if (capsule == null) {
            return responseGenerator.generateErrorResponse(request, HttpStatus.NOT_FOUND,
                    ResponseCode.CAPSULE_NOT_FOUND, MessageConstant.CAPSULE_NOT_FOUND, locale);
        }

        if (request.getContributorId() != null && !request.getContributorId().isBlank()) {
            InviteContributorDomain domain = new InviteContributorDomain(request.getContributorId().trim());
            return inviteContributor(normalizedCapsuleId, domain, locale);
        }

        Instant now = Instant.now(clock);
        String token = UUID.randomUUID().toString();
        Instant expiresAt = now.plus(7, ChronoUnit.DAYS);

        CapsuleInvite invite = CapsuleInvite.builder()
                .capsuleId(normalizedCapsuleId)
                .inviteToken(token)
                .inviterId(inviterId)
                .inviteeEmail(request.getInviteeEmail() != null ? request.getInviteeEmail().trim() : null)
                .status(InviteStatus.PENDING)
                .expiresAt(expiresAt)
                .createdAt(now)
                .build();

        capsuleInviteRepository.save(invite);

        if (request.getInviteeEmail() != null && !request.getInviteeEmail().isBlank()) {
            Optional<UserReg> targetUser = userRepository.findByEmail(request.getInviteeEmail().trim());
            if (targetUser.isPresent()) {
                notificationService.createNotification(
                        targetUser.get().getId(),
                        "Capsule Invite",
                        "You've been invited to contribute to '" + capsule.getTitle() + "'",
                        NotificationType.CAPSULE_INVITE
                );
            }
        }

        String inviteLink = "https://rootly.app/invite/" + token;
        InviteResponseDTO responseDTO = InviteResponseDTO.builder()
                .inviteToken(token)
                .inviteLink(inviteLink)
                .capsuleId(normalizedCapsuleId)
                .status(InviteStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        return responseGenerator.generateSuccessResponse(request, HttpStatus.CREATED,
                ResponseCode.CAPSULE_INVITE_SUCCESS, MessageConstant.CAPSULE_INVITE_SUCCESS, locale, responseDTO);
    }

    @Override
    @Transactional
    @SuppressWarnings("PMD.LawOfDemeter")
    public ResponseEntity<Object> joinByInvite(String inviteToken, String userId, Locale locale) {
        String token = requireId(inviteToken, "inviteToken is required");
        CapsuleInvite invite = capsuleInviteRepository.findByInviteToken(token).orElse(null);
        if (invite == null || invite.getStatus() != InviteStatus.PENDING) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.BAD_REQUEST,
                    ResponseCode.BAD_REQUEST, "Invalid or expired invite token", locale);
        }

        if (invite.getExpiresAt() != null && Instant.now(clock).isAfter(invite.getExpiresAt())) {
            invite.setStatus(InviteStatus.EXPIRED);
            capsuleInviteRepository.save(invite);
            return responseGenerator.generateErrorResponse(null, HttpStatus.BAD_REQUEST,
                    ResponseCode.BAD_REQUEST, "Invite link has expired", locale);
        }

        Capsule capsule = capsuleRepository.findById(invite.getCapsuleId()).orElse(null);
        if (capsule == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.CAPSULE_NOT_FOUND, MessageConstant.CAPSULE_NOT_FOUND, locale);
        }

        List<String> contributorIds = cleanIds(capsule.getContributorIds());
        if (!contributorIds.contains(userId) && !userId.equals(capsule.getCreatorId())) {
            contributorIds.add(userId);
            capsule.setContributorIds(contributorIds);
            capsule.setUpdatedAt(Instant.now(clock));
            capsuleRepository.save(capsule);
        }

        invite.setStatus(InviteStatus.ACCEPTED);
        capsuleInviteRepository.save(invite);

        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.RSP_SUCCESS, "Joined capsule successfully", null);
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
